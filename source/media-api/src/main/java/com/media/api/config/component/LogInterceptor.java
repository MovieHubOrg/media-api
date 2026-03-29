package com.media.api.config.component;

import com.media.api.dto.ApiMessageDto;
import com.media.api.exception.UnauthorizationException;
import com.media.api.jwt.BaseJwt;
import com.media.api.service.LoggingService;
import com.media.api.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class LogInterceptor implements HandlerInterceptor {
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    LoggingService loggingService;

    @Autowired
    UserServiceImpl userService;

    final static List<String> BYPASS_ENDPOINT = List.of(
//            "/v1/file/download-video-resource/**",
            "/v1/file/public-download/**",
            "/v1/file/download/**"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (DispatcherType.REQUEST.name().equals(request.getDispatcherType().name())
                && request.getMethod().equals(HttpMethod.GET.name())) {
            loggingService.logRequest(request, null);
        }
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        log.info("Starting call url: [" + getUrl(request) + "]");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);

        if (request.getAttribute("startTime") != null) {
            long startTime = (Long) request.getAttribute("startTime");
            long endTime = System.currentTimeMillis();
            long executeTime = endTime - startTime;
            log.debug("Complete [" + getUrl(request) + "] executeTime : " + executeTime + "ms");
        }

        if (ex != null) {
            log.info("afterCompletion>> " + ex.getMessage());

        }
    }


    /**
     * get full url request
     *
     * @param req
     * @return
     */
    private static String getUrl(HttpServletRequest req) {
        String reqUrl = req.getRequestURL().toString();
        String queryString = req.getQueryString();   // d=789
        if (!StringUtils.isEmpty(queryString)) {
            reqUrl += "?" + queryString;
        }
        return reqUrl;
    }

    private boolean isAllowed(HttpServletRequest request, List<String> whiteList) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
    }
}
