package com.example.elderlycare.modules.admission.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.admission.dto.BedDTO;
import com.example.elderlycare.modules.admission.dto.BuildingDTO;
import com.example.elderlycare.modules.admission.dto.RoomDTO;
import com.example.elderlycare.modules.admission.service.SpaceService;
import com.example.elderlycare.modules.admission.vo.BedBoardSummaryVO;
import com.example.elderlycare.modules.admission.vo.BedVO;
import com.example.elderlycare.modules.admission.vo.BuildingVO;
import com.example.elderlycare.modules.admission.vo.RoomVO;
import com.example.elderlycare.modules.admission.vo.SpaceTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "空间与床位管理", description = "楼栋、房间、床位资源与空间看板接口")
@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    // --- 楼栋管理 ---
    @Operation(summary = "查询楼栋列表")
    @GetMapping("/buildings")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<List<BuildingVO>> listBuildings() {
        return ApiResponse.success(spaceService.listBuildings());
    }

    @Operation(summary = "查询楼栋详情")
    @GetMapping("/buildings/{id}")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<BuildingVO> getBuildingById(@PathVariable Long id) {
        return ApiResponse.success(spaceService.getBuildingById(id));
    }

    @Operation(summary = "创建楼栋")
    @PostMapping("/buildings")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Long> createBuilding(@Valid @RequestBody BuildingDTO dto) {
        return ApiResponse.success(spaceService.createBuilding(dto));
    }

    @Operation(summary = "更新楼栋")
    @PutMapping("/buildings/{id}")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Void> updateBuilding(@PathVariable Long id, @Valid @RequestBody BuildingDTO dto) {
        spaceService.updateBuilding(id, dto);
        return ApiResponse.success();
    }

    // --- 房间管理 ---
    @Operation(summary = "查询房间列表")
    @GetMapping("/rooms")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<List<RoomVO>> listRooms(@RequestParam(required = false) Long buildingId) {
        return ApiResponse.success(spaceService.listRooms(buildingId));
    }

    @Operation(summary = "查询房间详情")
    @GetMapping("/rooms/{id}")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<RoomVO> getRoomById(@PathVariable Long id) {
        return ApiResponse.success(spaceService.getRoomById(id));
    }

    @Operation(summary = "创建房间")
    @PostMapping("/rooms")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Long> createRoom(@Valid @RequestBody RoomDTO dto) {
        return ApiResponse.success(spaceService.createRoom(dto));
    }

    @Operation(summary = "更新房间")
    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Void> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomDTO dto) {
        spaceService.updateRoom(id, dto);
        return ApiResponse.success();
    }

    // --- 床位管理 ---
    @Operation(summary = "查询床位列表")
    @GetMapping("/beds")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<List<BedVO>> listBeds(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(spaceService.listBeds(roomId, status));
    }

    @Operation(summary = "查询所有空闲可用床位")
    @GetMapping("/beds/available")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<List<BedVO>> listAvailableBeds() {
        return ApiResponse.success(spaceService.listAvailableBeds());
    }

    @Operation(summary = "新增床位")
    @PostMapping("/beds")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Long> createBed(@Valid @RequestBody BedDTO dto) {
        return ApiResponse.success(spaceService.createBed(dto));
    }

    @Operation(summary = "更新床位信息")
    @PutMapping("/beds/{id}")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Void> updateBed(@PathVariable Long id, @Valid @RequestBody BedDTO dto) {
        spaceService.updateBed(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "修改床位状态（如设置为维修、停用或恢复可用）")
    @PutMapping("/beds/{id}/status")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Void> updateBedStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        spaceService.updateBedStatus(id, status);
        return ApiResponse.success();
    }

    // --- 空间树与看板统计 ---
    @Operation(summary = "获取空间全景树（楼栋 -> 房间 -> 床位）")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<List<SpaceTreeVO>> getSpaceTree() {
        return ApiResponse.success(spaceService.getSpaceTree());
    }

    @Operation(summary = "获取床位看板汇总数据")
    @GetMapping("/bed-board/summary")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<BedBoardSummaryVO> getBedBoardSummary() {
        return ApiResponse.success(spaceService.getBedBoardSummary());
    }
}
