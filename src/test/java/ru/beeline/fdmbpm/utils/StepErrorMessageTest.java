package ru.beeline.fdmbpm.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.beeline.fdmbpm.exception.ExternalServiceException;

import static org.assertj.core.api.Assertions.assertThat;

class StepErrorMessageTest {

    private static final String STEP = "Публикация в Structurizr";
    private static final String GENERIC = "Внутренняя ошибка на шаге «Публикация в Structurizr». Обратитесь в поддержку";

    @Test
    @DisplayName("details из ответа сервиса — возвращается как есть")
    void usesDetails() {
        ExternalServiceException e = new ExternalServiceException("HTTP 400", 400, "  В workspace нет CMDB  ", null);

        assertThat(StepErrorMessage.of(STEP, e)).isEqualTo("В workspace нет CMDB");
    }

    @Test
    @DisplayName("ExternalServiceException глубже в цепочке cause — details находится")
    void findsDetailsInCauseChain() {
        ExternalServiceException ese = new ExternalServiceException("HTTP 404", 404, "Продукт не найден", null);

        assertThat(StepErrorMessage.of(STEP, new RuntimeException("wrapper", ese))).isEqualTo("Продукт не найден");
    }

    @Test
    @DisplayName("Длинный details обрезается до 1000 символов")
    void truncatesLongDetails() {
        ExternalServiceException e = new ExternalServiceException("HTTP 409", 409, "x".repeat(5000), null);

        assertThat(StepErrorMessage.of(STEP, e)).hasSize(StepErrorMessage.MAX_LENGTH);
    }

    @Test
    @DisplayName("Нет details (таймаут, 5xx без тела) — общий текст")
    void genericWithoutDetails() {
        assertThat(StepErrorMessage.of(STEP, new ExternalServiceException("timeout", null, null, null)))
                .isEqualTo(GENERIC);
        assertThat(StepErrorMessage.of(STEP, new ExternalServiceException("HTTP 500", 500, " ", null)))
                .isEqualTo(GENERIC);
    }

    @Test
    @DisplayName("Произвольное исключение — общий текст без технических подробностей")
    void genericForOtherErrors() {
        assertThat(StepErrorMessage.of(STEP, new NullPointerException("processId is null"))).isEqualTo(GENERIC);
    }
}
