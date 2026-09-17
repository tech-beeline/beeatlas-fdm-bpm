package ru.beeline.fdmbpm.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.smartkts.StructurizrObjectDTO;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SmartKtsClientTest {

    private static final String BASE_URL = "https://smartkts.example.com";
    private static final String ENDPOINT = BASE_URL + "/generate_by_structirizr_id_with_link";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("201 — тело StructurizrObject, bwiki_kts_link передаётся явным null, хвостовой / в адресе не дублируется")
    void generatesPassport() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.structurizr_id").value("98765"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"bwiki_kts_link\":null")))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"structurizr_id\":\"98765\",\"bwiki_kts_link\":null}"));

        StructurizrObjectDTO response = new SmartKtsClient(BASE_URL + "/", restTemplate)
                .generateKtsPassport("98765", null);

        server.verify();
        assertThat(response.getStructurizrId()).isEqualTo("98765");
        assertThat(response.getBwikiKtsLink()).isNull();
    }

    @Test
    @DisplayName("422 — исключение с кодом и телом ответа SmartKTS")
    void failsOnValidationError() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":[{\"msg\":\"Field required\"}]}"));

        SmartKtsClient client = new SmartKtsClient(BASE_URL, restTemplate);

        assertThatThrownBy(() -> client.generateKtsPassport("98765", "link"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 422")
                .hasMessageContaining("Field required");
    }

    @Test
    @DisplayName("5xx — исключение с кодом ответа")
    void failsOnServerError() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        SmartKtsClient client = new SmartKtsClient(BASE_URL, restTemplate);

        assertThatThrownBy(() -> client.generateKtsPassport("98765", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 502");
    }

    @Test
    @DisplayName("Таймаут или сетевая ошибка — исключение «SmartKTS недоступен»")
    void failsOnTimeout() {
        server.expect(requestTo(ENDPOINT)).andRespond(request -> {
            throw new SocketTimeoutException("Read timed out");
        });

        SmartKtsClient client = new SmartKtsClient(BASE_URL, restTemplate);

        assertThatThrownBy(() -> client.generateKtsPassport("98765", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SmartKTS недоступен")
                .hasMessageContaining("Read timed out");
    }

    @Test
    @DisplayName("Адрес SmartKTS не задан — исключение без HTTP-вызова")
    void failsWithoutUrl() {
        SmartKtsClient client = new SmartKtsClient("  ", restTemplate);

        assertThatThrownBy(() -> client.generateKtsPassport("98765", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("integration.smartkts.url");
        server.verify();
    }
}
