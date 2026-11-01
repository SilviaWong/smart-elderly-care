package com.example.elderlycare.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.system.dto.LoginRequest;
import com.example.elderlycare.modules.system.dto.UserCreateDTO;
import com.example.elderlycare.modules.system.dto.UserQueryDTO;
import com.example.elderlycare.modules.system.dto.UserUpdateDTO;
import com.example.elderlycare.modules.system.entity.SysOrg;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.entity.SysUserRole;
import com.example.elderlycare.modules.system.mapper.SysOrgMapper;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.mapper.SysUserRoleMapper;
import com.example.elderlycare.modules.system.service.SysUserService;
import com.example.elderlycare.modules.system.vo.LoginResponse;
import com.example.elderlycare.modules.system.vo.UserVO;
import com.example.elderlycare.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysOrgMapper sysOrgMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())
        );

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户账号已被停用");
        }

        // 机构归属检查
        if (request.getOrgId() != null && user.getOrgId() != null && !request.getOrgId().equals(user.getOrgId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "所选机构与用户所属机构不匹配");
        }

        // 查询角色与权限编码
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissionCodes = sysUserMapper.selectPermissionCodesByUserId(user.getId());

        // 机构名称
        String orgName = "平台超级管理";
        if (user.getOrgId() != null) {
            SysOrg org = sysOrgMapper.selectById(user.getOrgId());
            if (org != null) {
                orgName = org.getOrgName();
            }
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        // 签发 JWT
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getOrgId(), roleCodes);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .orgId(user.getOrgId())
                .orgName(orgName)
                .roles(roleCodes)
                .permissions(permissionCodes)
                .build();
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = OrgContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "当前用户不存在");
        }

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        return convertToVO(user, roles);
    }

    @Override
    public PageResult<UserVO> pageUsers(UserQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 强制根据当前用户机构做多租户数据隔离
        wrapper.eq(currentOrgId != null, SysUser::getOrgId, currentOrgId);
        wrapper.like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername());
        wrapper.like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName());
        wrapper.eq(StringUtils.hasText(query.getStatus()), SysUser::getStatus, query.getStatus());
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<UserVO> list = page.getRecords().stream().map(u -> {
            List<String> roles = sysUserMapper.selectRoleCodesByUserId(u.getId());
            return convertToVO(u, roles);
        }).collect(Collectors.toList());

        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 检查用户名在当前机构内唯一性
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(currentOrgId != null, SysUser::getOrgId, currentOrgId)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        SysUser user = SysUser.builder()
                .orgId(currentOrgId)
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .realName(dto.getRealName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        sysUserMapper.insert(user);

        // 保存角色关联
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                sysUserRoleMapper.insert(SysUserRole.builder().userId(user.getId()).roleId(roleId).build());
            }
        }

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserUpdateDTO dto) {
        SysUser user = checkAndGetUserWithOrgCheck(id);

        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        if (StringUtils.hasText(dto.getStatus())) {
            user.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        sysUserMapper.updateById(user);

        // 重新分配角色
        if (dto.getRoleIds() != null) {
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            for (Long roleId : dto.getRoleIds()) {
                sysUserRoleMapper.insert(SysUserRole.builder().userId(user.getId()).roleId(roleId).build());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUserStatus(Long id, String status) {
        SysUser user = checkAndGetUserWithOrgCheck(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    private SysUser checkAndGetUserWithOrgCheck(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        Long currentOrgId = OrgContextHolder.getOrgId();
        // 跨机构越权防线：如果当前用户属于某个机构，但操作的用户属于不同机构，直接阻断
        if (currentOrgId != null && !currentOrgId.equals(user.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问或修改其他机构的用户数据");
        }

        return user;
    }

    private UserVO convertToVO(SysUser user, List<String> roles) {
        return UserVO.builder()
                .id(user.getId())
                .orgId(user.getOrgId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(roles != null ? roles : new ArrayList<>())
                .build();
    }
}
