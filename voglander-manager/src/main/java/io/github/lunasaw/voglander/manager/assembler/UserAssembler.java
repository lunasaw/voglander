package io.github.lunasaw.voglander.manager.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.vo.UserInfoVO;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 用户数据转换器
 *
 * @author luna
 */
public class UserAssembler {

    /**
     * DO转DTO
     */
    public static UserDTO toDTO(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(userDO.getId());
        dto.setUsername(userDO.getUsername());
        dto.setPassword(userDO.getPassword());
        dto.setNickname(userDO.getNickname());
        dto.setEmail(userDO.getEmail());
        dto.setPhone(userDO.getPhone());
        dto.setAvatar(userDO.getAvatar());
        dto.setStatus(userDO.getStatus());
        dto.setLastLogin(userDO.getLastLogin());
        return dto;
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