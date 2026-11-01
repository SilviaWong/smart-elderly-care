package com.example.elderlycare.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.system.dto.RoleCreateDTO;
import com.example.elderlycare.modules.system.dto.RoleUpdateDTO;
import com.example.elderlycare.modules.system.entity.SysRole;
import com.example.elderlycare.modules.system.entity.SysRolePermission;
import com.example.elderlycare.modules.system.entity.SysUserRole;
import com.example.elderlycare.modules.system.mapper.SysRoleMapper;
import com.example.elderlycare.modules.system.mapper.SysRolePermissionMapper;
import com.example.elderlycare.modules.system.mapper.SysUserRoleMapper;
import com.example.elderlycare.modules.system.service.SysRoleService;
import com.example.elderlycare.modules.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<RoleVO> listRoles() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            wrapper.eq(SysRole::getOrgId, currentOrgId).or().isNull(SysRole::getOrgId);
        }
        wrapper.orderByAsc(SysRole::getId);

        List<SysRole> list = sysRoleMapper.selectList(wrapper);
        return list.stream().map(role -> {
            List<Long> permIds = sysRoleMapper.selectPermissionIdsByRoleId(role.getId());
            return RoleVO.builder()
                    .id(role.getId())
                    .orgId(role.getOrgId())
                    .roleCode(role.getRoleCode())
                    .roleName(role.getRoleName())
                    .description(role.getDescription())
                    .permissionIds(permIds)
                    .createdAt(role.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        Long count = sysRoleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, dto.getRoleCode())
                        .eq(currentOrgId != null, SysRole::getOrgId, currentOrgId)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码在当前机构已存在");
        }

        SysRole role = SysRole.builder()
                .orgId(currentOrgId)
                .roleCode(dto.getRoleCode())
                .roleName(dto.getRoleName())
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        sysRoleMapper.insert(role);

        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            for (Long permId : dto.getPermissionIds()) {
                sysRolePermissionMapper.insert(
                        SysRolePermission.builder().roleId(role.getId()).permissionId(permId).build()
                );
            }
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleUpdateDTO dto) {
        SysRole role = checkAndGetRole(id);
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        sysRoleMapper.updateById(role);

        if (dto.getPermissionIds() != null) {
            sysRolePermissionMapper.delete(
                    new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, role.getId())
            );
            for (Long permId : dto.getPermissionIds()) {
                sysRolePermissionMapper.insert(
                        SysRolePermission.builder().roleId(role.getId()).permissionId(permId).build()
                );
            }
        }
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        checkAndGetRole(roleId);
        return sysRoleMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = checkAndGetRole(id);

        Long userCount = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, role.getId())
        );
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该角色下仍有关联用户，禁止删除");
        }

        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, role.getId())
        );
        sysRoleMapper.deleteById(role.getId());
    }

    private SysRole checkAndGetRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
        }

        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && role.getOrgId() != null && !currentOrgId.equals(role.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问或修改其他机构的角色数据");
        }

        return role;
    }
}
