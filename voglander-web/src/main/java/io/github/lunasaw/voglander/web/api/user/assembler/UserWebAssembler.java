package io.github.lunasaw.voglander.web.api.user.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.web.api.user.req.UserCreateReq;
import io.github.lunasaw.voglander.web.api.user.req.UserQueryReq;
import io.github.lunasaw.voglander.web.api.user.req.UserUpdateReq;
import io.github.lunasaw.voglander.web.api.role.vo.RoleVO;
import io.github.lunasaw.voglander.web.api.user.vo.UserInfoVO;
import io.github.lunasaw.voglander.web.api.user.vo.UserListResp;
import io.github.lunasaw.voglander.web.api.user.vo.UserVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户Web层数据转换器
 *
 * @author luna
 */
public class UserWebAssembler {

    /**
     * UserQueryReq转UserDTO
     */
    public static UserDTO toDTO(UserQueryReq req) {
        if (req == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setUsername(req.getUsername());
        dto.setNickname(req.getNickname());
        dto.setEmail(req.getEmail());
        dto.setPhone(req.getPhone());
        dto.setStatus(req.getStatus());
        dto.setPageNum(req.getPageNum());
        dto.setPageSize(req.getPageSize());
        return dto;
    }

    /**
     * UserCreateReq转UserDTO
     */
    public static UserDTO toDTO(UserCreateReq req) {
        if (req == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setUsername(req.getUsername());
        dto.setPassword(req.getPassword());
        dto.setNickname(req.getNickname());
        dto.setEmail(req.getEmail());
        dto.setPhone(req.getPhone());
        dto.setAvatar(req.getAvatar());
        dto.setStatus(req.getStatus());
        dto.setRoleIds(req.getRoleIds());
        return dto;
    }

    /**
     * UserUpdateReq转UserDTO
     */
    public static UserDTO toDTO(UserUpdateReq req) {
        if (req == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(req.getId());
        if (StringUtils.isNotBlank(req.getPassword())) {
            dto.setPassword(req.getPassword());
        }
        dto.setNickname(req.getNickname());
        dto.setEmail(req.getEmail());
        dto.setPhone(req.getPhone());
        dto.setAvatar(req.getAvatar());
        dto.setStatus(req.getStatus());
        dto.setRoleIds(req.getRoleIds());
        return dto;
    }

    /**
     * UserDTO转UserVO
     */
    public static UserVO toVO(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(dto.getId());
        vo.setUsername(dto.getUsername());
        vo.setNickname(dto.getNickname());
        vo.setEmail(dto.getEmail());
        vo.setPhone(dto.getPhone());
        vo.setAvatar(dto.getAvatar());
        vo.setStatus(dto.getStatus());
        vo.setLastLogin(dto.getLastLogin());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setRoleIds(dto.getRoleIds());

        // 转换角色信息
        if (dto.getRoles() != null) {
            List<RoleVO> roleVOList = dto.getRoles().stream()
                .map(UserWebAssembler::roleDTO2VO)
                .collect(Collectors.toList());
            vo.setRoles(roleVOList);
        }

        return vo;
    }

    /**
     * RoleDTO转RoleVO
     */
    private static RoleVO roleDTO2VO(RoleDTO roleDTO) {
        if (roleDTO == null) {
            return null;
        }
        RoleVO roleVO = new RoleVO();
        roleVO.setId(roleDTO.getId());
        roleVO.setName(roleDTO.getRoleName());
        roleVO.setRemark(roleDTO.getDescription());
        roleVO.setStatus(roleDTO.getStatus());
        roleVO.setCreateTime(roleDTO.getCreateTime());
        roleVO.setUpdateTime(roleDTO.getUpdateTime());
        return roleVO;
    }

    /**
     * UserDTO列表转UserVO列表
     */
    public static List<UserVO> toVOList(List<UserDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(UserWebAssembler::toVO).collect(Collectors.toList());
    }

    /**
     * UserVO列表转UserListResp
     */
    public static UserListResp toListResp(List<UserVO> voList) {
        UserListResp resp = new UserListResp();
        resp.setItems(voList != null ? voList : Collections.emptyList());
        return resp;
    }

    /**
     * DTO转UserInfoVO
     */
    public static UserInfoVO toUserInfoVO(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(userDTO.getId());
        vo.setUsername(userDTO.getUsername());
        vo.setRealName(StringUtils.isNotBlank(userDTO.getNickname()) ? userDTO.getNickname() : userDTO.getUsername());
        vo.setAvatar(StringUtils.isNotBlank(userDTO.getAvatar()) ? userDTO.getAvatar() : "");
        vo.setDesc("管理员");
        vo.setHomePath("/dashboard");

        // 设置角色信息 - 改为简单字符串数组，匹配前端期望
        vo.setRoles(Collections.singletonList("admin"));

        return vo;
    }
}