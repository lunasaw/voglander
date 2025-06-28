package io.github.lunasaw.voglander.web.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptReq;
import io.github.lunasaw.voglander.manager.domaon.vo.DeptResp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门Web层转换器
 * 负责 Req -> DTO 和 DTO -> Resp 的转换
 *
 * @author luna
 */
@Component
public class DeptWebAssembler {

    /**
     * DeptReq转换为DeptDTO
     *
     * @param deptReq 部门请求对象
     * @return DeptDTO
     */
    public DeptDTO toDTO(DeptReq deptReq) {
        if (deptReq == null) {
            return null;
        }

        DeptDTO dto = new DeptDTO();
        dto.setDeptName(deptReq.getName());
        dto.setRemark(deptReq.getRemark());
        dto.setStatus(deptReq.getStatus());
        dto.setSortOrder(deptReq.getSortOrder());
        dto.setLeader(deptReq.getLeader());
        dto.setPhone(deptReq.getPhone());
        dto.setEmail(deptReq.getEmail());

        // 转换父级ID
        if (StringUtils.isNotBlank(deptReq.getParentId())) {
            try {
                dto.setParentId(Long.valueOf(deptReq.getParentId()));
            } catch (NumberFormatException e) {
                dto.setParentId(0L); // 默认为根节点
            }
        } else {
            dto.setParentId(0L);
        }

        return dto;
    }

    /**
     * DeptDTO转换为DeptResp
     *
     * @param deptDTO 部门DTO对象
     * @return DeptResp
     */
    public DeptResp toResp(DeptDTO deptDTO) {
        if (deptDTO == null) {
            return null;
        }

        DeptResp resp = new DeptResp();
        resp.setId(String.valueOf(deptDTO.getId()));
        resp.setName(deptDTO.getDeptName());
        resp.setRemark(deptDTO.getRemark());
        resp.setStatus(deptDTO.getStatus());
        resp.setSortOrder(deptDTO.getSortOrder());
        resp.setLeader(deptDTO.getLeader());
        resp.setPhone(deptDTO.getPhone());
        resp.setEmail(deptDTO.getEmail());
        resp.setCreateTime(deptDTO.getCreateTime());

        // 转换父级ID
        if (deptDTO.getParentId() != null) {
            resp.setParentId(String.valueOf(deptDTO.getParentId()));
        }

        // 转换子部门
        if (deptDTO.getChildren() != null && !deptDTO.getChildren().isEmpty()) {
            List<DeptResp> children = deptDTO.getChildren().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
            resp.setChildren(children);
        }

        return resp;
    }

    /**
     * DeptDTO列表转换为DeptResp列表
     *
     * @param deptDTOList 部门DTO列表
     * @return DeptResp列表
     */
    public List<DeptResp> toRespList(List<DeptDTO> deptDTOList) {
        if (deptDTOList == null || deptDTOList.isEmpty()) {
            return Collections.emptyList();
        }
        return deptDTOList.stream().map(this::toResp).collect(Collectors.toList());
    }
}