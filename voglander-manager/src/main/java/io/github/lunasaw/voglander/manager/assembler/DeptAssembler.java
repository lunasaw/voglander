package io.github.lunasaw.voglander.manager.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.repository.entity.DeptDO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门数据转换器
 *
 * @author luna
 */
@Component
public class DeptAssembler {

    /**
     * DO转DTO
     */
    public static DeptDTO toDTO(DeptDO deptDO) {
        if (deptDO == null) {
            return null;
        }
        DeptDTO dto = new DeptDTO();
        dto.setId(deptDO.getId());
        dto.setParentId(deptDO.getParentId());
        dto.setDeptName(deptDO.getDeptName());
        dto.setDeptCode(deptDO.getDeptCode());
        dto.setRemark(deptDO.getRemark());
        dto.setStatus(deptDO.getStatus());
        dto.setSortOrder(deptDO.getSortOrder());
        dto.setLeader(deptDO.getLeader());
        dto.setPhone(deptDO.getPhone());
        dto.setEmail(deptDO.getEmail());

        // 转换创建时间
        if (deptDO.getCreateTime() != null) {
            dto.setCreateTime(deptDO.getCreateTime().toString());
        }

        return dto;
    }

    /**
     * DO列表转DTO列表
     */
    public static List<DeptDTO> toDTOList(List<DeptDO> deptDOList) {
        if (deptDOList == null || deptDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return deptDOList.stream().map(DeptAssembler::toDTO).collect(Collectors.toList());
    }

    /**
     * DTO转DO
     */
    public static DeptDO toDO(DeptDTO deptDTO) {
        if (deptDTO == null) {
            return null;
        }
        DeptDO deptDO = new DeptDO();
        deptDO.setId(deptDTO.getId());
        deptDO.setParentId(deptDTO.getParentId());
        deptDO.setDeptName(deptDTO.getDeptName());
        deptDO.setDeptCode(deptDTO.getDeptCode());
        deptDO.setRemark(deptDTO.getRemark());
        deptDO.setStatus(deptDTO.getStatus());
        deptDO.setSortOrder(deptDTO.getSortOrder());
        deptDO.setLeader(deptDTO.getLeader());
        deptDO.setPhone(deptDTO.getPhone());
        deptDO.setEmail(deptDTO.getEmail());
        return deptDO;
    }

    /**
     * 构建部门树
     */
    public static List<DeptDTO> buildDeptTree(List<DeptDTO> deptList) {
        if (deptList == null || deptList.isEmpty()) {
            return Collections.emptyList();
        }

        // 按父ID分组
        Map<Long, List<DeptDTO>> parentDeptMap = deptList.stream()
            .collect(Collectors.groupingBy(DeptDTO::getParentId));

        // 找出根部门并构建树（父ID为0或null的为根部门）
        List<DeptDTO> rootDepts = parentDeptMap.getOrDefault(0L, Collections.emptyList());
        List<DeptDTO> nullParentDepts = parentDeptMap.getOrDefault(null, Collections.emptyList());

        // 合并根部门
        rootDepts.addAll(nullParentDepts);

        buildChildren(rootDepts, parentDeptMap);

        return rootDepts;
    }

    /**
     * 递归构建子部门
     */
    private static void buildChildren(List<DeptDTO> parentDepts, Map<Long, List<DeptDTO>> parentDeptMap) {
        if (parentDepts == null || parentDepts.isEmpty()) {
            return;
        }

        for (DeptDTO parentDept : parentDepts) {
            List<DeptDTO> children = parentDeptMap.getOrDefault(parentDept.getId(), Collections.emptyList());
            if (!children.isEmpty()) {
                parentDept.setChildren(children);
                buildChildren(children, parentDeptMap);
            }
        }
    }
}