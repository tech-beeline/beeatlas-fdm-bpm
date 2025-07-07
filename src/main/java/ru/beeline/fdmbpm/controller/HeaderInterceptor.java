package ru.beeline.fdmbpm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static ru.beeline.fdmbpm.utils.Constants.*;

public class HeaderInterceptor implements HandlerInterceptor {
    private Logger logger = LoggerFactory.getLogger(HeaderInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!request.getRequestURI().contains("/application/")
                || request.getRequestURI().contains("/actuator")
                || request.getRequestURI().contains("/swagger")
                || request.getRequestURI().contains("/error")
                || request.getRequestURI().contains("/api-docs")
                || request.getRequestURI().contains("/change-status/")
                || request.getRequestURI().contains("/sync-order")) {
            return true;
        }
        List<String> missingHeaders = new ArrayList<>();
        if (isBlank(request.getHeader(USER_ID_HEADER))) missingHeaders.add(USER_ID_HEADER);
        if (isBlank(request.getHeader(USER_PERMISSION_HEADER))) missingHeaders.add(USER_PERMISSION_HEADER);
        if (isBlank(request.getHeader(USER_PRODUCTS_IDS_HEADER))) missingHeaders.add(USER_PRODUCTS_IDS_HEADER);
        if (isBlank(request.getHeader(USER_ROLES_HEADER))) missingHeaders.add(USER_ROLES_HEADER);
        if (!missingHeaders.isEmpty()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Отсутствуют необходимые заголовки.");
            return false;
        }
        Map<String, Object> headers = new HashMap<>();
        logger.info(USER_ID_HEADER + ": " + request.getHeader(USER_ID_HEADER));
        headers.put(USER_ID_HEADER, request.getHeader(USER_ID_HEADER));
        logger.info(USER_PERMISSION_HEADER + ": " + request.getHeader(USER_PERMISSION_HEADER));
        headers.put(USER_PERMISSION_HEADER, toList(request.getHeader(USER_PERMISSION_HEADER)));
        logger.info(USER_PRODUCTS_IDS_HEADER + ": " + request.getHeader(USER_PRODUCTS_IDS_HEADER));
        headers.put(USER_PRODUCTS_IDS_HEADER, toList(request.getHeader(USER_PRODUCTS_IDS_HEADER)));
        logger.info(USER_ROLES_HEADER + ": " + request.getHeader(USER_ROLES_HEADER));
        headers.put(USER_ROLES_HEADER, toList(request.getHeader(USER_ROLES_HEADER)));
        RequestContext.setHeaders(headers);
        logger.info("Set headers complete");
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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