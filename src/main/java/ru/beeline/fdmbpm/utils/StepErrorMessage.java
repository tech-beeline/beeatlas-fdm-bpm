/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.utils;

import ru.beeline.fdmbpm.exception.ExternalServiceException;

/**
 * Текст причины ошибки шага для пользователя (camunda_process_status.message).
 * Берёт details из ответа соседнего сервиса; если его нет — общий текст без технических подробностей.
 */
public final class StepErrorMessage {

    public static final int MAX_LENGTH = 1000;

    private StepErrorMessage() {
    }

    public static String of(String stepName, Throwable error) {
        for (Throwable e = error; e != null; e = e.getCause()) {
            if (e instanceof ExternalServiceException ese && ese.getDetails() != null && !ese.getDetails().isBlank()) {
                String details = ese.getDetails().strip();
                return details.length() > MAX_LENGTH ? details.substring(0, MAX_LENGTH) : details;
            }
        }
        return String.format("Внутренняя ошибка на шаге «%s». Обратитесь в поддержку", stepName);
    }
}
