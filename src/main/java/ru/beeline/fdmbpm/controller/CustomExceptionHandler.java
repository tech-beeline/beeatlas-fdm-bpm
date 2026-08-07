/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.controller;

import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.fdmbpm.dto.ErrorMessageDTO;
import ru.beeline.fdmbpm.exception.*;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleException(ValidationException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<Object> handleException(S3Exception e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleException(ResponseStatusException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleException(NotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException e) {
        String paramName = e.getParameterName();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageDTO("Параметр '" + paramName + "' обязателен"));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> headerException(ForbiddenException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(CustomCamundaException.class)
    public ResponseEntity<Object> headerException(CustomCamundaException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(ProcessException.class)
    public ResponseEntity<Object> headerException(ProcessException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessageDTO(e.getMessage()));
    }
}
