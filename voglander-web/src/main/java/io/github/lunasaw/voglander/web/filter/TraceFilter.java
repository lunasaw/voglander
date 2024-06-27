package io.github.lunasaw.voglander.web.filter;

import java.io.IOException;

import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import io.github.lunasaw.voglander.common.constant.Constants;
import jakarta.servlet.*;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author luna
 * @date 2024/6/27
 */
public class TraceFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        HttpServletResponse resp = (HttpServletResponse)servletResponse;

        String traceId = TraceContext.traceId();
        filterChain.doFilter(req, resp);
        resp.setHeader(Constants.SKY_WALKING_TID, traceId);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
