package com.example.elderlycare.modules.admission.service;

import com.example.elderlycare.modules.admission.dto.BedDTO;
import com.example.elderlycare.modules.admission.dto.BuildingDTO;
import com.example.elderlycare.modules.admission.dto.RoomDTO;
import com.example.elderlycare.modules.admission.vo.BedBoardSummaryVO;
import com.example.elderlycare.modules.admission.vo.BedVO;
import com.example.elderlycare.modules.admission.vo.BuildingVO;
import com.example.elderlycare.modules.admission.vo.RoomVO;
import com.example.elderlycare.modules.admission.vo.SpaceTreeVO;

import java.util.List;

public interface SpaceService {

    // 楼栋
    List<BuildingVO> listBuildings();

    BuildingVO getBuildingById(Long id);

    Long createBuilding(BuildingDTO dto);

    void updateBuilding(Long id, BuildingDTO dto);

    // 房间
    List<RoomVO> listRooms(Long buildingId);

    RoomVO getRoomById(Long id);

    Long createRoom(RoomDTO dto);

    void updateRoom(Long id, RoomDTO dto);

    // 床位
    List<BedVO> listBeds(Long roomId, String status);

    List<BedVO> listAvailableBeds();

    Long createBed(BedDTO dto);

    void updateBed(Long id, BedDTO dto);

    void updateBedStatus(Long id, String status);

    // 空间树与看板汇总
    List<SpaceTreeVO> getSpaceTree();

    BedBoardSummaryVO getBedBoardSummary();
}
