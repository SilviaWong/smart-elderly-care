package com.example.elderlycare.modules.system.service;

import com.example.elderlycare.modules.system.vo.PermissionVO;

import java.util.List;

public interface SysPermissionService {

    List<PermissionVO> getPermissionTree();
}
