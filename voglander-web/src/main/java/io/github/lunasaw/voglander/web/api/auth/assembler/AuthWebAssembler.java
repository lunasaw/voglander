package io.github.lunasaw.voglander.web.api.auth.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.LoginDTO;
import io.github.lunasaw.voglander.web.api.auth.vo.LoginVO;
import io.github.lunasaw.voglander.web.api.auth.vo.LoginReq;
import io.github.lunasaw.voglander.web.api.auth.vo.LoginResp;
import io.github.lunasaw.voglander.web.api.auth.vo.RefreshTokenResp;

/**
 * 认证Web层数据转换器
 *
 * @author luna
 */
public class AuthWebAssembler {

    /**
     * LoginReq转LoginDTO
     */
    public static LoginDTO toLoginDTO(LoginReq loginReq) {
        if (loginReq == null) {
            return null;
        }
        LoginDTO dto = new LoginDTO();
        dto.setUsername(loginReq.getUsername());
        dto.setPassword(loginReq.getPassword());
        return dto;
    }

    /**
     * LoginVO转LoginResp
     */
    public static LoginResp toLoginResp(LoginVO loginVO) {
        if (loginVO == null) {
            return null;
        }
        LoginResp resp = new LoginResp();
        resp.setAccessToken(loginVO.getAccessToken());
        return resp;
    }

    /**
     * token转RefreshTokenResp
     */
    public static RefreshTokenResp toRefreshTokenResp(String token) {
        RefreshTokenResp resp = new RefreshTokenResp();
        resp.setData(token);
        resp.setStatus(200);
        return resp;
    }
}