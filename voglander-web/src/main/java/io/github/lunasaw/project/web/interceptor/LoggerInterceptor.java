package io.github.lunasaw.project.web.interceptor;

import org.springframework.util.StopWatch;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoggerInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        URL url = new URL(URLDecoder.decode(request.getRequestURL().toString(), StandardCharsets.UTF_8.name()));
        String host = url.getHost();
        String uri = url.getPath();
        String remoteIp = request.getRemoteAddr();
        Map<String, String[]> params = request.getParameterMap();
        Cookie[] cookies = request.getCookies();
        request.setAttribute("access_host", host);
        request.setAttribute("access_uri", uri);
        request.setAttribute("access_remoteIp", remoteIp);
        request.setAttribute("access_params", params);
        request.setAttribute("access_cookies", cookies);
        StopWatch sw = new StopWatch();
        sw.start();
        request.setAttribute("access_sw", sw);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) throws Exception {
        String host = (String) request.getAttribute("access_host");
        String uri = (String) request.getAttribute("access_uri");
        String remoteIp = (String) request.getAttribute("access_remoteIp");
        Map<String, String[]> params = (Map<String, String[]>) request.getAttribute("access_params");
        Cookie[] cookies = (Cookie[]) request.getAttribute("access_cookies");
        StopWatch sw = (StopWatch) request.getAttribute("access_sw");
        sw.stop();


    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws Exception {
    }
}
