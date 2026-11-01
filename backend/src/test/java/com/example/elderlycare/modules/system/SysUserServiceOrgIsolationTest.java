package com.example.elderlycare.modules.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.system.dto.UserCreateDTO;
import com.example.elderlycare.modules.system.dto.UserUpdateDTO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysOrgMapper;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.mapper.SysUserRoleMapper;
import com.example.elderlycare.modules.system.service.impl.SysUserServiceImpl;
import com.example.elderlycare.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysUserServiceOrgIsolationTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysOrgMapper sysOrgMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("创建用户时密码必须经过加密存储，且自动绑定当前机构ID")
    void shouldEncryptPasswordAndBindOrgIdWhenCreatingUser() {
        OrgContextHolder.setOrgId(1L);

        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("plaintext123")).thenReturn("$2a$10$encryptedHashPassword");

        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("testuser");
        dto.setPassword("plaintext123");
        dto.setRealName("测试用户");

        sysUserService.createUser(dto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());

        SysUser savedUser = captor.getValue();
        assertEquals(1L, savedUser.getOrgId(), "新建用户必须自动绑定当前机构 ID");
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("$2a$10$encryptedHashPassword", savedUser.getPasswordHash(), "用户密码绝不能明文存储");
        assertNotEquals("plaintext123", savedUser.getPasswordHash());
    }

    @Test
    @DisplayName("机构A用户试图修改机构B用户时，必须抛出 ORG_ACCESS_DENIED 异常被拦截")
    void shouldThrowOrgAccessDeniedWhenAccessingOtherOrgUser() {
        // 当前登录用户属于机构 1
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(10L);

        // 目标用户属于机构 2
        SysUser targetUserInOrg2 = SysUser.builder()
                .id(99L)
                .orgId(2L)
                .username("org2_user")
                .realName("机构2员工")
                .build();

        when(sysUserMapper.selectById(99L)).thenReturn(targetUserInOrg2);

        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setRealName("篡改姓名");

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            sysUserService.updateUser(99L, updateDTO);
        });

        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("无权访问或修改其他机构"));
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }
}
