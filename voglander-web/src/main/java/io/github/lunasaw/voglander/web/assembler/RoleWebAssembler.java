package io.github.lunasaw.voglander.web.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.domaon.vo.RoleVO;
import io.github.lunasaw.voglander.web.api.role.req.RoleCreateReq;
import io.github.lunasaw.voglander.web.api.role.req.RoleQueryReq;
import io.github.lunasaw.voglander.web.api.role.req.RoleUpdateReq;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色Web层数据转换器
 *
 * @author luna
 */
public class RoleWebAssembler {

    /**
     * 查询请求转DTO
     */
    public static RoleDTO toDTO(RoleQueryReq req) {
        if (req == null) {
            return new RoleDTO();
        }

        RoleDTO dto = new RoleDTO();
        dto.setRoleName(req.getName());
        dto.setRoleCode(req.getRoleCode());
        dto.setStatus(req.getStatus());
        return dto;
    }

    /**
     * 创建请求转DTO
     */
    public static RoleDTO toDTO(RoleCreateReq req) {
        if (req == null) {
            return null;
        }

        RoleDTO dto = new RoleDTO();
        dto.setRoleName(req.getName());
        dto.setRoleCode(req.getRoleCode());
        dto.setDescription(req.getRemark());
        dto.setStatus(req.getStatus());
        dto.setPermissions(req.getPermissions());
        return dto;
    }

    /**
     * 更新请求转DTO
     */
    public static RoleDTO toDTO(RoleUpdateReq req) {
        if (req == null) {
            return null;
        }

        RoleDTO dto = new RoleDTO();
        dto.setRoleName(req.getName());
        dto.setRoleCode(req.getRoleCode());
        dto.setDescription(req.getRemark());
        dto.setStatus(req.getStatus());
        dto.setPermissions(req.getPermissions());
        return dto;
    }

    /**
     * DTO转VO
     */
    public static RoleVO toVO(RoleDTO dto) {
        if (dto == null) {
            return null;
        }

        RoleVO vo = new RoleVO();
        vo.setId(dto.getId() != null ? dto.getId().toString() : null);
        vo.setName(dto.getRoleName());
        vo.setRoleCode(dto.getRoleCode());
        vo.setRemark(dto.getDescription());
        vo.setStatus(dto.getStatus());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setPermissions(dto.getPermissions());
        return vo;
    }

    /**
     * DTO列表转VO列表
     */
    public static List<RoleVO> toVOList(List<RoleDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream().map(RoleWebAssembler::toVO).collect(Collectors.toList());
    }
}