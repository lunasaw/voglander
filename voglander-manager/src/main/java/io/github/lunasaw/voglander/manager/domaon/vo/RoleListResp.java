package io.github.lunasaw.voglander.manager.domaon.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 角色列表响应VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RoleListResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色列表
     */
    private List<RoleVO>      items;
}