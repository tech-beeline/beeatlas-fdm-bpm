/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.beeline.fdmbpm.dto.ErrorResponse;
import ru.beeline.fdmbpm.exception.NotFoundException;

@RestControllerAdvice(assignableTypes = PipelineController.class)
public class PipelineExceptionHandler {

    static final String BAD_TYPE_PROCESS_ID_MESSAGE = "Параметр typeProcessId обязателен и должен быть целым числом.";
    static final String METHOD_NOT_ALLOWED_MESSAGE = "Метод не разрешён для данного ресурса.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadParam(IllegalArgumentException ex) {
        if ("typeProcessId".equals(ex.getMessage())) {
            return error(HttpStatus.BAD_REQUEST, BAD_TYPE_PROCESS_ID_MESSAGE);
        }
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("typeProcessId".equals(ex.getName())) {
            return error(HttpStatus.BAD_REQUEST, BAD_TYPE_PROCESS_ID_MESSAGE);
        }
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_MESSAGE);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(message));
    }
}

