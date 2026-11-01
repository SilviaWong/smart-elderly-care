package com.example.elderlycare.modules.admission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.dto.BedDTO;
import com.example.elderlycare.modules.admission.dto.BuildingDTO;
import com.example.elderlycare.modules.admission.dto.RoomDTO;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.entity.Building;
import com.example.elderlycare.modules.admission.entity.Room;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.admission.service.SpaceService;
import com.example.elderlycare.modules.admission.vo.BedBoardSummaryVO;
import com.example.elderlycare.modules.admission.vo.BedVO;
import com.example.elderlycare.modules.admission.vo.BuildingVO;
import com.example.elderlycare.modules.admission.vo.RoomVO;
import com.example.elderlycare.modules.admission.vo.SpaceTreeVO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceServiceImpl implements SpaceService {

    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final BedMapper bedMapper;
    private final AdmissionMapper admissionMapper;
    private final ElderMapper elderMapper;

    @Override
    public List<BuildingVO> listBuildings() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        List<Building> buildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>()
                        .eq(currentOrgId != null, Building::getOrgId, currentOrgId)
                        .orderByAsc(Building::getBuildingCode)
        );

        return buildings.stream().map(b -> {
            Long roomCount = roomMapper.selectCount(
                    new LambdaQueryWrapper<Room>()
                            .eq(Room::getBuildingId, b.getId())
            );
            List<Room> rooms = roomMapper.selectList(
                    new LambdaQueryWrapper<Room>().eq(Room::getBuildingId, b.getId())
            );
            int bedCount = 0;
            if (!rooms.isEmpty()) {
                List<Long> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());
                bedCount = Math.toIntExact(bedMapper.selectCount(
                        new LambdaQueryWrapper<Bed>().in(Bed::getRoomId, roomIds)
                ));
            }

            return BuildingVO.builder()
                    .id(b.getId())
                    .orgId(b.getOrgId())
                    .buildingName(b.getBuildingName())
                    .buildingCode(b.getBuildingCode())
                    .address(b.getAddress())
                    .status(b.getStatus())
                    .roomCount(Math.toIntExact(roomCount))
                    .bedCount(bedCount)
                    .createdAt(b.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public BuildingVO getBuildingById(Long id) {
        Building b = checkBuildingOrgAccess(id);
        return BuildingVO.builder()
                .id(b.getId())
                .orgId(b.getOrgId())
                .buildingName(b.getBuildingName())
                .buildingCode(b.getBuildingCode())
                .address(b.getAddress())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBuilding(BuildingDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 校验机构内楼栋编号唯一
        Long count = buildingMapper.selectCount(
                new LambdaQueryWrapper<Building>()
                        .eq(currentOrgId != null, Building::getOrgId, currentOrgId)
                        .eq(Building::getBuildingCode, dto.getBuildingCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "楼栋编码 [" + dto.getBuildingCode() + "] 在当前机构已存在");
        }

        Building building = Building.builder()
                .orgId(currentOrgId)
                .buildingName(dto.getBuildingName())
                .buildingCode(dto.getBuildingCode())
                .address(dto.getAddress())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        buildingMapper.insert(building);
        return building.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBuilding(Long id, BuildingDTO dto) {
        Building building = checkBuildingOrgAccess(id);
        Long currentOrgId = OrgContextHolder.getOrgId();

        if (!building.getBuildingCode().equals(dto.getBuildingCode())) {
            Long count = buildingMapper.selectCount(
                    new LambdaQueryWrapper<Building>()
                            .eq(currentOrgId != null, Building::getOrgId, currentOrgId)
                            .eq(Building::getBuildingCode, dto.getBuildingCode())
                            .ne(Building::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "楼栋编码 [" + dto.getBuildingCode() + "] 在当前机构已存在");
            }
            building.setBuildingCode(dto.getBuildingCode());
        }

        building.setBuildingName(dto.getBuildingName());
        building.setAddress(dto.getAddress());
        if (StringUtils.hasText(dto.getStatus())) {
            building.setStatus(dto.getStatus());
        }
        buildingMapper.updateById(building);
    }

    @Override
    public List<RoomVO> listRooms(Long buildingId) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, Room::getOrgId, currentOrgId);
        wrapper.eq(buildingId != null, Room::getBuildingId, buildingId);
        wrapper.orderByAsc(Room::getFloorNo).orderByAsc(Room::getRoomNo);

        List<Room> rooms = roomMapper.selectList(wrapper);
        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Building> buildingMap = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().eq(currentOrgId != null, Building::getOrgId, currentOrgId)
        ).stream().collect(Collectors.toMap(Building::getId, b -> b));

        return rooms.stream().map(r -> {
            Building b = buildingMap.get(r.getBuildingId());
            Long totalBeds = bedMapper.selectCount(new LambdaQueryWrapper<Bed>().eq(Bed::getRoomId, r.getId()));
            Long availableBeds = bedMapper.selectCount(
                    new LambdaQueryWrapper<Bed>()
                            .eq(Bed::getRoomId, r.getId())
                            .eq(Bed::getStatus, "AVAILABLE")
            );

            return RoomVO.builder()
                    .id(r.getId())
                    .orgId(r.getOrgId())
                    .buildingId(r.getBuildingId())
                    .buildingName(b != null ? b.getBuildingName() : "--")
                    .floorNo(r.getFloorNo())
                    .roomNo(r.getRoomNo())
                    .roomType(r.getRoomType())
                    .status(r.getStatus())
                    .bedCount(Math.toIntExact(totalBeds))
                    .availableBedCount(Math.toIntExact(availableBeds))
                    .createdAt(r.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public RoomVO getRoomById(Long id) {
        Room r = checkRoomOrgAccess(id);
        Building b = buildingMapper.selectById(r.getBuildingId());
        Long totalBeds = bedMapper.selectCount(new LambdaQueryWrapper<Bed>().eq(Bed::getRoomId, r.getId()));
        Long availableBeds = bedMapper.selectCount(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getRoomId, r.getId())
                        .eq(Bed::getStatus, "AVAILABLE")
        );

        return RoomVO.builder()
                .id(r.getId())
                .orgId(r.getOrgId())
                .buildingId(r.getBuildingId())
                .buildingName(b != null ? b.getBuildingName() : "--")
                .floorNo(r.getFloorNo())
                .roomNo(r.getRoomNo())
                .roomType(r.getRoomType())
                .status(r.getStatus())
                .bedCount(Math.toIntExact(totalBeds))
                .availableBedCount(Math.toIntExact(availableBeds))
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoom(RoomDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        // 校验楼栋归属
        checkBuildingOrgAccess(dto.getBuildingId());

        // 校验房间号唯一性 (org_id, building_id, room_no)
        Long count = roomMapper.selectCount(
                new LambdaQueryWrapper<Room>()
                        .eq(currentOrgId != null, Room::getOrgId, currentOrgId)
                        .eq(Room::getBuildingId, dto.getBuildingId())
                        .eq(Room::getRoomNo, dto.getRoomNo())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该楼栋下已存在房间号 [" + dto.getRoomNo() + "]");
        }

        Room room = Room.builder()
                .orgId(currentOrgId)
                .buildingId(dto.getBuildingId())
                .floorNo(dto.getFloorNo())
                .roomNo(dto.getRoomNo())
                .roomType(dto.getRoomType())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        roomMapper.insert(room);
        return room.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(Long id, RoomDTO dto) {
        Room room = checkRoomOrgAccess(id);
        Long currentOrgId = OrgContextHolder.getOrgId();

        if (!room.getRoomNo().equals(dto.getRoomNo()) || !room.getBuildingId().equals(dto.getBuildingId())) {
            Long count = roomMapper.selectCount(
                    new LambdaQueryWrapper<Room>()
                            .eq(currentOrgId != null, Room::getOrgId, currentOrgId)
                            .eq(Room::getBuildingId, dto.getBuildingId())
                            .eq(Room::getRoomNo, dto.getRoomNo())
                            .ne(Room::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "目标楼栋下已存在房间号 [" + dto.getRoomNo() + "]");
            }
            room.setBuildingId(dto.getBuildingId());
            room.setRoomNo(dto.getRoomNo());
        }

        room.setFloorNo(dto.getFloorNo());
        room.setRoomType(dto.getRoomType());
        if (StringUtils.hasText(dto.getStatus())) {
            room.setStatus(dto.getStatus());
        }
        roomMapper.updateById(room);
    }

    @Override
    public List<BedVO> listBeds(Long roomId, String status) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<Bed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, Bed::getOrgId, currentOrgId);
        wrapper.eq(roomId != null, Bed::getRoomId, roomId);
        wrapper.eq(StringUtils.hasText(status), Bed::getStatus, status);
        wrapper.orderByAsc(Bed::getBedNo);

        List<Bed> beds = bedMapper.selectList(wrapper);
        return convertBedList(beds);
    }

    @Override
    public List<BedVO> listAvailableBeds() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        List<Bed> beds = bedMapper.selectList(
                new LambdaQueryWrapper<Bed>()
                        .eq(currentOrgId != null, Bed::getOrgId, currentOrgId)
                        .eq(Bed::getStatus, "AVAILABLE")
                        .orderByAsc(Bed::getBedNo)
        );
        return convertBedList(beds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBed(BedDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        checkRoomOrgAccess(dto.getRoomId());

        Long count = bedMapper.selectCount(
                new LambdaQueryWrapper<Bed>()
                        .eq(currentOrgId != null, Bed::getOrgId, currentOrgId)
                        .eq(Bed::getRoomId, dto.getRoomId())
                        .eq(Bed::getBedNo, dto.getBedNo())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该房间下已存在床位号 [" + dto.getBedNo() + "]");
        }

        Bed bed = Bed.builder()
                .orgId(currentOrgId)
                .roomId(dto.getRoomId())
                .bedNo(dto.getBedNo())
                .bedType(dto.getBedType())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "AVAILABLE")
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        bedMapper.insert(bed);
        return bed.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBed(Long id, BedDTO dto) {
        Bed bed = checkBedOrgAccess(id);
        Long currentOrgId = OrgContextHolder.getOrgId();

        if (!bed.getBedNo().equals(dto.getBedNo()) || !bed.getRoomId().equals(dto.getRoomId())) {
            Long count = bedMapper.selectCount(
                    new LambdaQueryWrapper<Bed>()
                            .eq(currentOrgId != null, Bed::getOrgId, currentOrgId)
                            .eq(Bed::getRoomId, dto.getRoomId())
                            .eq(Bed::getBedNo, dto.getBedNo())
                            .ne(Bed::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "目标房间下已存在床位号 [" + dto.getBedNo() + "]");
            }
            bed.setRoomId(dto.getRoomId());
            bed.setBedNo(dto.getBedNo());
        }

        bed.setBedType(dto.getBedType());
        if (StringUtils.hasText(dto.getStatus())) {
            bed.setStatus(dto.getStatus());
        }
        bed.setUpdatedAt(LocalDateTime.now());
        bedMapper.updateById(bed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBedStatus(Long id, String status) {
        Bed bed = checkBedOrgAccess(id);
        // 如果床位已被在住长者占用，禁止直接转为维修或停用状态，需先办理调床或退住
        if ("OCCUPIED".equals(bed.getStatus()) && !"OCCUPIED".equals(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "床位处于占用状态，无法直接变更状态，请先办理调床或退住");
        }
        bed.setStatus(status);
        bed.setUpdatedAt(LocalDateTime.now());
        bedMapper.updateById(bed);
    }

    @Override
    public List<SpaceTreeVO> getSpaceTree() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        List<Building> buildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>()
                        .eq(currentOrgId != null, Building::getOrgId, currentOrgId)
                        .orderByAsc(Building::getBuildingCode)
        );

        if (buildings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Room> allRooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>()
                        .eq(currentOrgId != null, Room::getOrgId, currentOrgId)
                        .orderByAsc(Room::getFloorNo).orderByAsc(Room::getRoomNo)
        );

        List<Bed> allBeds = bedMapper.selectList(
                new LambdaQueryWrapper<Bed>()
                        .eq(currentOrgId != null, Bed::getOrgId, currentOrgId)
                        .orderByAsc(Bed::getBedNo)
        );

        List<BedVO> bedVOs = convertBedList(allBeds);
        Map<Long, List<BedVO>> bedMapByRoom = bedVOs.stream().collect(Collectors.groupingBy(BedVO::getRoomId));
        Map<Long, List<Room>> roomsByBuilding = allRooms.stream().collect(Collectors.groupingBy(Room::getBuildingId));

        List<SpaceTreeVO> tree = new ArrayList<>();
        for (Building b : buildings) {
            List<Room> bRooms = roomsByBuilding.getOrDefault(b.getId(), Collections.emptyList());
            List<SpaceTreeVO.RoomNodeVO> roomNodes = new ArrayList<>();

            int totalBeds = 0;
            int occupiedBeds = 0;
            int availableBeds = 0;

            for (Room r : bRooms) {
                List<BedVO> rBeds = bedMapByRoom.getOrDefault(r.getId(), Collections.emptyList());
                totalBeds += rBeds.size();
                for (BedVO bed : rBeds) {
                    if ("OCCUPIED".equals(bed.getStatus())) occupiedBeds++;
                    else if ("AVAILABLE".equals(bed.getStatus())) availableBeds++;
                }

                roomNodes.add(SpaceTreeVO.RoomNodeVO.builder()
                        .id(r.getId())
                        .buildingId(r.getBuildingId())
                        .floorNo(r.getFloorNo())
                        .roomNo(r.getRoomNo())
                        .roomType(r.getRoomType())
                        .status(r.getStatus())
                        .beds(rBeds)
                        .build());
            }

            tree.add(SpaceTreeVO.builder()
                    .id(b.getId())
                    .name(b.getBuildingName())
                    .code(b.getBuildingCode())
                    .address(b.getAddress())
                    .status(b.getStatus())
                    .totalRooms(bRooms.size())
                    .totalBeds(totalBeds)
                    .occupiedBeds(occupiedBeds)
                    .availableBeds(availableBeds)
                    .rooms(roomNodes)
                    .build());
        }

        return tree;
    }

    @Override
    public BedBoardSummaryVO getBedBoardSummary() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        List<Bed> beds = bedMapper.selectList(
                new LambdaQueryWrapper<Bed>()
                        .eq(currentOrgId != null, Bed::getOrgId, currentOrgId)
        );

        int total = beds.size();
        int occupied = 0;
        int available = 0;
        int maintenance = 0;
        int disabled = 0;

        for (Bed b : beds) {
            switch (b.getStatus()) {
                case "OCCUPIED" -> occupied++;
                case "AVAILABLE" -> available++;
                case "MAINTENANCE" -> maintenance++;
                case "DISABLED" -> disabled++;
            }
        }

        BigDecimal rate = BigDecimal.ZERO;
        if (total > 0) {
            rate = BigDecimal.valueOf(occupied)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        return BedBoardSummaryVO.builder()
                .totalBeds(total)
                .occupiedBeds(occupied)
                .availableBeds(available)
                .maintenanceBeds(maintenance)
                .disabledBeds(disabled)
                .occupancyRate(rate)
                .build();
    }

    private Building checkBuildingOrgAccess(Long buildingId) {
        Building b = buildingMapper.selectById(buildingId);
        if (b == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "楼栋不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(b.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的楼栋");
        }
        return b;
    }

    private Room checkRoomOrgAccess(Long roomId) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "房间不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(r.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的房间");
        }
        return r;
    }

    private Bed checkBedOrgAccess(Long bedId) {
        Bed b = bedMapper.selectById(bedId);
        if (b == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "床位不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(b.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的床位");
        }
        return b;
    }

    private List<BedVO> convertBedList(List<Bed> beds) {
        if (beds.isEmpty()) {
            return Collections.emptyList();
        }

        Long currentOrgId = OrgContextHolder.getOrgId();
        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>().eq(currentOrgId != null, Room::getOrgId, currentOrgId)
        );
        Map<Long, Room> roomMap = rooms.stream().collect(Collectors.toMap(Room::getId, r -> r));

        List<Building> buildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().eq(currentOrgId != null, Building::getOrgId, currentOrgId)
        );
        Map<Long, Building> buildingMap = buildings.stream().collect(Collectors.toMap(Building::getId, b -> b));

        // 查询当前正在占用的有效入住记录
        List<Admission> activeAdmissions = admissionMapper.selectList(
                new LambdaQueryWrapper<Admission>()
                        .eq(currentOrgId != null, Admission::getOrgId, currentOrgId)
                        .eq(Admission::getStatus, "ACTIVE")
        );
        Map<Long, Admission> admissionByBedId = activeAdmissions.stream()
                .collect(Collectors.toMap(Admission::getBedId, a -> a, (k1, k2) -> k1));

        Map<Long, Elder> elderMap = new HashMap<>();
        if (!activeAdmissions.isEmpty()) {
            List<Long> elderIds = activeAdmissions.stream().map(Admission::getElderId).collect(Collectors.toList());
            List<Elder> elders = elderMapper.selectBatchIds(elderIds);
            elderMap = elders.stream().collect(Collectors.toMap(Elder::getId, e -> e));
        }

        final Map<Long, Elder> finalElderMap = elderMap;

        return beds.stream().map(b -> {
            Room r = roomMap.get(b.getRoomId());
            Building bld = r != null ? buildingMap.get(r.getBuildingId()) : null;
            Admission adm = admissionByBedId.get(b.getId());
            Elder elder = (adm != null) ? finalElderMap.get(adm.getElderId()) : null;

            return BedVO.builder()
                    .id(b.getId())
                    .orgId(b.getOrgId())
                    .roomId(b.getRoomId())
                    .roomNo(r != null ? r.getRoomNo() : "--")
                    .buildingId(r != null ? r.getBuildingId() : null)
                    .buildingName(bld != null ? bld.getBuildingName() : "--")
                    .floorNo(r != null ? r.getFloorNo() : "--")
                    .bedNo(b.getBedNo())
                    .bedType(b.getBedType())
                    .status(b.getStatus())
                    .version(b.getVersion())
                    .currentAdmissionId(adm != null ? adm.getId() : null)
                    .currentElderId(elder != null ? elder.getId() : null)
                    .currentElderName(elder != null ? elder.getName() : null)
                    .currentElderNo(elder != null ? elder.getElderNo() : null)
                    .admissionDate(adm != null ? adm.getAdmissionDate() : null)
                    .createdAt(b.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
