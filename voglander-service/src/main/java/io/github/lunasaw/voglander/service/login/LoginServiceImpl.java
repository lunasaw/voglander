package io.github.lunasaw.voglander.service.login;

import io.github.lunasaw.voglander.client.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author luna
 * @date 2023/12/29
 */
@Slf4j
@Service
public class LoginServiceImpl implements LoginService {


    @Override
    public void login() {
        log.info("login::");
    }

    @Override
    public void keepalive() {

    }
}
