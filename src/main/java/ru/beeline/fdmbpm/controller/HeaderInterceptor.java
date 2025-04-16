package ru.beeline.fdmbpm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;
import java.util.stream.Collectors;

import static ru.beeline.fdmbpm.utils.Constants.*;

public class HeaderInterceptor implements HandlerInterceptor {
    private Logger logger = LoggerFactory.getLogger(HeaderInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if (!request.getRequestURI().contains("/application/")) {
                return true;
            }
            Map<String, Object> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                logger.debug(headerName + ": " + headerValue);
            }
            logger.info(USER_ID_HEADER + request.getHeader(USER_ID_HEADER));
            headers.put(USER_ID_HEADER, request.getHeader(USER_ID_HEADER).toString());
            logger.info(USER_PERMISSION_HEADER + toList(request.getHeader(USER_PERMISSION_HEADER)));
            headers.put(USER_PERMISSION_HEADER, toList(request.getHeader(USER_PERMISSION_HEADER).toString()));
            logger.info(USER_PRODUCTS_IDS_HEADER + toList(request.getHeader(USER_PRODUCTS_IDS_HEADER)));
            headers.put(USER_PRODUCTS_IDS_HEADER, toList(request.getHeader(USER_PRODUCTS_IDS_HEADER).toString()));
            logger.info(USER_ROLES_HEADER + toList(request.getHeader(USER_ROLES_HEADER)));
            headers.put(USER_ROLES_HEADER, toList(request.getHeader(USER_ROLES_HEADER).toString()));
            RequestContext.setHeaders(headers);
            logger.info("Set headers complete");
            return true;
        } catch (Exception e) {
            throw new ForbiddenException("Отсутсвуют необходимые хэдеры.");
        }
    }

    private List<String> toList(String value) {
        return Arrays.stream(value.split(","))
                .map(str -> str.substring(0))
                .map(str -> str.replaceAll("\"", ""))
                .map(str -> str.replaceAll("]", ""))
                .map(str -> str.replaceAll("\\[", ""))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}