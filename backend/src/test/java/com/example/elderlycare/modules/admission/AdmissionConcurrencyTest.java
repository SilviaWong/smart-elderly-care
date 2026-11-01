package com.example.elderlycare.modules.admission;

import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.dto.AdmissionCheckInDTO;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedChangeLogMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.admission.service.impl.AdmissionServiceImpl;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.ElderService;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionConcurrencyTest {

    @Mock
    private AdmissionMapper admissionMapper;

    @Mock
    private BedMapper bedMapper;

    @Mock
    private BedChangeLogMapper bedChangeLogMapper;

    @Mock
    private BuildingMapper buildingMapper;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private ElderMapper elderMapper;

    @Mock
    private ElderService elderService;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysOperationLogService sysOperationLogService;

    @InjectMocks
    private AdmissionServiceImpl admissionService;

    @Test
    @DisplayName("高并发抢占同一床位：10个线程同时发起入住，必须且仅有1个成功，其余9个安全失败并提示床位被占用")
    void shouldAllowOnlyOneCheckInWhenMultipleThreadsCompeteForSameBed() throws InterruptedException {
        int threadCount = 10;
        Long targetBedId = 888L;

        // 模拟数据库中行锁和单床位真实状态
        final Bed sharedBedState = Bed.builder()
                .id(targetBedId)
                .orgId(1L)
                .roomId(10L)
                .bedNo("VIP-888")
                .status("AVAILABLE")
                .version(0)
                .build();

        // 模拟 MySQL InnoDB 行级互斥排他锁 (SELECT ... FOR UPDATE)
        ReentrantLock dbRowLock = new ReentrantLock();

        when(bedMapper.selectByIdForUpdate(targetBedId)).thenAnswer(invocation -> {
            // 获取排他行锁
            dbRowLock.lock();
            // 返回当前床位最新状态的快照副本
            return Bed.builder()
                    .id(sharedBedState.getId())
                    .orgId(sharedBedState.getOrgId())
                    .roomId(sharedBedState.getRoomId())
                    .bedNo(sharedBedState.getBedNo())
                    .status(sharedBedState.getStatus())
                    .version(sharedBedState.getVersion())
                    .build();
        });

        // 当 updateById 被调用时，同步修改共享床位状态并释放锁（模拟事务提交）
        doAnswer(invocation -> {
            Bed updated = invocation.getArgument(0);
            sharedBedState.setStatus(updated.getStatus());
            sharedBedState.setVersion(updated.getVersion());
            if (dbRowLock.isHeldByCurrentThread()) {
                dbRowLock.unlock();
            }
            return 1;
        }).when(bedMapper).updateById(any(Bed.class));

        // 模拟不同长者查询
        when(elderService.getElderEntityById(anyLong())).thenAnswer(invocation -> {
            Long elderId = invocation.getArgument(0);
            return Elder.builder()
                    .id(elderId)
                    .orgId(1L)
                    .elderNo("ELD-" + elderId)
                    .name("长者-" + elderId)
                    .status("PENDING")
                    .build();
        });

        when(admissionMapper.selectCount(any())).thenReturn(0L);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final long elderId = 1000L + i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // 等待所有线程全部就绪后同时发令起跑
                    startLatch.await();
                    OrgContextHolder.setOrgId(1L);
                    OrgContextHolder.setUserId(elderId);

                    AdmissionCheckInDTO dto = AdmissionCheckInDTO.builder()
                            .elderId(elderId)
                            .bedId(targetBedId)
                            .admissionDate(LocalDate.now())
                            .depositAmount(new BigDecimal("2000.00"))
                            .build();

                    admissionService.checkIn(dto);
                    successCount.incrementAndGet();
                } catch (BusinessException ex) {
                    if (dbRowLock.isHeldByCurrentThread()) {
                        dbRowLock.unlock();
                    }
                    if (ex.getCode() == ErrorCode.CONFLICT.getCode()) {
                        conflictCount.incrementAndGet();
                    } else {
                        unexpectedErrorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    if (dbRowLock.isHeldByCurrentThread()) {
                        dbRowLock.unlock();
                    }
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    OrgContextHolder.clear();
                    doneLatch.countDown();
                }
            });
        }

        // 等待所有线程就绪
        readyLatch.await(5, TimeUnit.SECONDS);
        // 发令启动高并发竞争
        startLatch.countDown();
        // 等待所有线程执行完毕
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "并发任务应在10秒内全部执行完成");
        assertEquals(0, unexpectedErrorCount.get(), "不应产生任何未预期的异常");
        assertEquals(1, successCount.get(), "高并发抢占同一床位时，必须且只能有 1 个请求成功");
        assertEquals(threadCount - 1, conflictCount.get(), "未能抢到床位的其余 " + (threadCount - 1) + " 个请求必须安全返回 CONFLICT 业务错误");

        // 最终数据库中床位状态必须为 OCCUPIED，版本号增加 1
        assertEquals("OCCUPIED", sharedBedState.getStatus());
        assertEquals(1, sharedBedState.getVersion());

        // 校验入住单插入恰好 1 次
        verify(admissionMapper, times(1)).insert(any(Admission.class));
    }
}
