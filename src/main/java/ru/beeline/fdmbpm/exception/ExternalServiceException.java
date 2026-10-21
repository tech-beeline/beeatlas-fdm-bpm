/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.exception;

import lombok.Getter;

/**
 * Ошибка вызова соседнего сервиса. {@code status} и {@code details} — HTTP-код и поле {@code details} из тела ответа,
 * оба {@code null}, если ответа не было (таймаут, сеть).
 */
@Getter
public class ExternalServiceException extends RuntimeException {

    private final Integer status;
    private final String details;

    public ExternalServiceException(String message, Integer status, String details, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.details = details;
    }
}
