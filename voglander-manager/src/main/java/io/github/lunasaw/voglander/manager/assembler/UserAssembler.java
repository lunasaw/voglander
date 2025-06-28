package io.github.lunasaw.voglander.manager.assembler;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.vo.UserInfoVO;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        dto.setCreateTime(userDO.getCreateTime());
        dto.setUpdateTime(userDO.getUpdateTime());
        return dto;
    }

    /**
     * DTO转DO
     */
    public static UserDO toDO(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        userDO.setId(userDTO.getId());
        userDO.setUsername(userDTO.getUsername());
        userDO.setPassword(userDTO.getPassword());
        userDO.setNickname(userDTO.getNickname());
        userDO.setEmail(userDTO.getEmail());
        userDO.setPhone(userDTO.getPhone());
        userDO.setAvatar(userDTO.getAvatar());
        userDO.setStatus(userDTO.getStatus());
        userDO.setLastLogin(userDTO.getLastLogin());
        userDO.setCreateTime(userDTO.getCreateTime());
        userDO.setUpdateTime(userDTO.getUpdateTime());
        return userDO;
    }

    /**
     * DO列表转DTO列表
     */
    public static List<UserDTO> toDTOList(List<UserDO> userDOList) {
        if (userDOList == null || userDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return userDOList.stream().map(UserAssembler::toDTO).collect(Collectors.toList());
    }

    /**
     * DO分页转DTO分页
     */
    public static IPage<UserDTO> toDTOPage(IPage<UserDO> userDOPage) {
        if (userDOPage == null) {
            return null;
        }
        IPage<UserDTO> dtoPage = new Page<>(userDOPage.getCurrent(), userDOPage.getSize(), userDOPage.getTotal());
        dtoPage.setRecords(toDTOList(userDOPage.getRecords()));
        return dtoPage;
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