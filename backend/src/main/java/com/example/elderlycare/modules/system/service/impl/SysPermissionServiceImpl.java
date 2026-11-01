package com.example.elderlycare.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.modules.system.entity.SysPermission;
import com.example.elderlycare.modules.system.mapper.SysPermissionMapper;
import com.example.elderlycare.modules.system.service.SysPermissionService;
import com.example.elderlycare.modules.system.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public List<PermissionVO> getPermissionTree() {
        List<SysPermission> all = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getId)
        );

        Map<Long, PermissionVO> map = new HashMap<>();
        List<PermissionVO> roots = new ArrayList<>();

        for (SysPermission p : all) {
            PermissionVO vo = PermissionVO.builder()
                    .id(p.getId())
                    .parentId(p.getParentId())
                    .permissionCode(p.getPermissionCode())
                    .permissionName(p.getPermissionName())
                    .permissionType(p.getPermissionType())
                    .path(p.getPath())
                    .children(new ArrayList<>())
                    .build();
            map.put(p.getId(), vo);
        }

        for (SysPermission p : all) {
            PermissionVO vo = map.get(p.getId());
            if (p.getParentId() == null || !map.containsKey(p.getParentId())) {
                roots.add(vo);
            } else {
                map.get(p.getParentId()).getChildren().add(vo);
            }
        }

        return roots;
    }
}
