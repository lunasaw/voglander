package io.github.lunasaw.voglander.web.api.user.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.web.api.user.req.UserCreateReq;
import io.github.lunasaw.voglander.web.api.user.req.UserQueryReq;
import io.github.lunasaw.voglander.web.api.user.req.UserUpdateReq;
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
        return vo;
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
}