package ru.beeline.fdmbpm.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.staging.PipelineRunDTO;
import ru.beeline.fdmbpm.dto.staging.PipelineRunStatusDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class StagingClientTest {

    private static final String BASE_URL = "http://staging.example.com";

    private MockRestServiceServer server;
    private MockRestServiceServer longServer;
    private StagingClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        RestTemplate longTimeoutRestTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        longServer = MockRestServiceServer.bindTo(longTimeoutRestTemplate).build();
        client = new StagingClient(BASE_URL, restTemplate, longTimeoutRestTemplate);
    }

    @Test
    @DisplayName("POST pipeline-runs — тело artifactType, artifactUid, source=manual, branch, payload.docId; 202 с runId")
    void startsPipelineRun() {
        server.expect(requestTo(BASE_URL + "/api/v1/pipeline-runs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.artifactType").value("structurizr-architecture"))
                .andExpect(jsonPath("$.artifactUid").value("BC-1"))
                .andExpect(jsonPath("$.source").value("manual"))
                .andExpect(jsonPath("$.branch").value("develop"))
                .andExpect(jsonPath("$.payload.docId").value(77))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"runId\":501,\"status\":\"pending\"}"));

        ResponseEntity<PipelineRunDTO> response =
                client.startPipelineRun("structurizr-architecture", "BC-1", "develop", 77);

        server.verify();
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody().getRunId()).isEqualTo(501L);
    }

    @Test
    @DisplayName("POST pipeline-runs 409 — ответ с кодом без исключения")
    void returnsErrorStatusWithoutException() {
        server.expect(requestTo(BASE_URL + "/api/v1/pipeline-runs"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"active run\",\"activeRunId\":3}"));

        ResponseEntity<PipelineRunDTO> response =
                client.startPipelineRun("structurizr-sequence", "BC-1", "main", 77);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("GET status?waitFor=terminal идёт через long-timeout RestTemplate и возвращает статус")
    void readsRunStatus() {
        longServer.expect(requestTo(BASE_URL + "/api/v1/pipeline-runs/501/status?waitFor=terminal"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"runId\":501,\"artifactType\":\"structurizr-sequence\",\"artifactUid\":\"BC-1\","
                                + "\"status\":\"awaiting_review\",\"stage\":\"review\",\"noticesCount\":0,"
                                + "\"result\":null,\"more\":true}"));

        PipelineRunStatusDTO status = client.waitPipelineRunTerminal(501L);

        longServer.verify();
        assertThat(status.getStatus()).isEqualTo("awaiting_review");
        assertThat(status.isMore()).isTrue();
    }

    @Test
    @DisplayName("GET status 404 — исключение")
    void failsWhenRunNotFound() {
        longServer.expect(requestTo(BASE_URL + "/api/v1/pipeline-runs/9/status?waitFor=terminal"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.waitPipelineRunTerminal(9L)).isInstanceOf(RuntimeException.class);
    }
}
