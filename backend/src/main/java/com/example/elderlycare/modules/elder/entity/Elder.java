package com.example.elderlycare.modules.elder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("elder")
public class Elder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String elderNo;

    private String name;

    private String gender;

    private LocalDate birthDate;

    private String idType;

    private String idNoCiphertext;

    private String idNoHash;

    private String phone;

    private String address;

    private String photoUrl;

    private String bloodType;

    private String allergyDesc;

    private String chronicDiseaseDesc;

    private String medicationDesc;

    private String dietTaboo;

    private String mobilityLevel;

    private String status;

    private String remark;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
