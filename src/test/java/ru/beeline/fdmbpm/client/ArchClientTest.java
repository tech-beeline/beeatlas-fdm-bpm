package ru.beeline.fdmbpm.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.exception.ExternalServiceException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ArchClientTest {

    private static final String BASE_URL = "https://arch.example.com";
    private static final String STRUCTURIZR_ENDPOINT = BASE_URL + "/api/v1/workspace/55416";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ArchClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new ArchClient(BASE_URL, restTemplate, restTemplate);
    }

    @Test
    @DisplayName("201 — публикация в Structurizr без исключения")
    void publishesToStructurizr() {
        server.expect(requestTo(STRUCTURIZR_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"details\":\"Ok\",\"workspace_id\":\"7033425664\"}"));

        client.publicToStructurizr(55416);

        server.verify();
    }

    @Test
    @DisplayName("409 с details — исключение с кодом и текстом details")
    void failsWithDetails() {
        server.expect(requestTo(STRUCTURIZR_ENDPOINT))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"details\":\"Внутренняя ошибка публикации в Structurizr\",\"workspace_id\":\"7033425664\"}"));

        ExternalServiceException e = catchThrowableOfType(() -> client.publicToStructurizr(55416),
                ExternalServiceException.class);

        assertThat(e.getStatus()).isEqualTo(409);
        assertThat(e.getDetails()).isEqualTo("Внутренняя ошибка публикации в Structurizr");
        assertThat(e).hasMessageContaining("HTTP 409");
        assertThat(e.getCause()).isNotNull();
    }

    @Test
    @DisplayName("404 в формате FastAPI (detail) — текст берётся из detail")
    void failsWithFastApiDetail() {
        server.expect(requestTo(BASE_URL + "/api/v1/workspace/55416/fdm"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Продукт не найден\"}"));

        ExternalServiceException e = catchThrowableOfType(() -> client.publicFdm(55416),
                ExternalServiceException.class);

        assertThat(e.getStatus()).isEqualTo(404);
        assertThat(e.getDetails()).isEqualTo("Продукт не найден");
    }

    @Test
    @DisplayName("500 без тела — исключение с кодом, details = null")
    void failsWithoutBody() {
        server.expect(requestTo(STRUCTURIZR_ENDPOINT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ExternalServiceException e = catchThrowableOfType(() -> client.publicToStructurizr(55416),
                ExternalServiceException.class);

        assertThat(e.getStatus()).isEqualTo(500);
        assertThat(e.getDetails()).isNull();
    }

    @Test
    @DisplayName("Тело не JSON — details = null")
    void failsWithNonJsonBody() {
        server.expect(requestTo(STRUCTURIZR_ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_HTML)
                        .body("<html>502 Bad Gateway</html>"));

        ExternalServiceException e = catchThrowableOfType(() -> client.publicToStructurizr(55416),
                ExternalServiceException.class);

        assertThat(e.getStatus()).isEqualTo(502);
        assertThat(e.getDetails()).isNull();
    }

    @Test
    @DisplayName("Таймаут — исключение без кода и details")
    void failsOnTimeout() {
        server.expect(requestTo(STRUCTURIZR_ENDPOINT)).andRespond(request -> {
            throw new SocketTimeoutException("Read timed out");
        });

        assertThatThrownBy(() -> client.publicToStructurizr(55416))
                .isInstanceOfSatisfying(ExternalServiceException.class, e -> {
                    assertThat(e.getStatus()).isNull();
                    assertThat(e.getDetails()).isNull();
                })
                .hasMessageContaining("Read timed out");
    }
}
