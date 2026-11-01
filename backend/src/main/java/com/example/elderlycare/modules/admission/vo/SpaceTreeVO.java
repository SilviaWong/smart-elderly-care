package com.example.elderlycare.modules.admission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceTreeVO {

    private Long id;

    private String name;

    private String code;

    private String address;

    private String status;

    private Integer totalRooms;

    private Integer totalBeds;

    private Integer occupiedBeds;

    private Integer availableBeds;

    private List<RoomNodeVO> rooms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomNodeVO {
        private Long id;
        private Long buildingId;
        private String floorNo;
        private String roomNo;
        private String roomType;
        private String status;
        private List<BedVO> beds;
    }
}
