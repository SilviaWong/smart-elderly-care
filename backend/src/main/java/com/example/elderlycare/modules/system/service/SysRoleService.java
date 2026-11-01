package com.example.elderlycare.modules.system.service;

import com.example.elderlycare.modules.system.dto.RoleCreateDTO;
import com.example.elderlycare.modules.system.dto.RoleUpdateDTO;
import com.example.elderlycare.modules.system.vo.RoleVO;

import java.util.List;

public interface SysRoleService {

    List<RoleVO> listRoles();

    Long createRole(RoleCreateDTO dto);

    void updateRole(Long id, RoleUpdateDTO dto);

    List<Long> getRolePermissionIds(Long roleId);

    void deleteRole(Long id);
}
