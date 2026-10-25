/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.staging.PipelineRunDTO;
import ru.beeline.fdmbpm.dto.staging.PipelineRunRequestDTO;
import ru.beeline.fdmbpm.dto.staging.PipelineRunStatusDTO;

import java.util.Set;

@Slf4j
@Service
public class StagingClient {

    public static final Set<String> TERMINAL_STATUSES =
            Set.of("completed", "failed", "cancelled", "completed_without_publish");

    private static final String MANUAL_SOURCE = "manual";

    private final RestTemplate restTemplate;
    // Long-poll статуса staging держит ответ до 30 секунд по умолчанию, это равно таймауту чтения restTemplate.
    private final RestTemplate longTimeoutRestTemplate;
    private final String stagingUrl;

    public StagingClient(@Value("${integration.staging-service-url}") String stagingUrl,
                         RestTemplate restTemplate,
                         @Qualifier("longTimeoutRestTemplate") RestTemplate longTimeoutRestTemplate) {
        this.stagingUrl = stagingUrl;
        this.restTemplate = restTemplate;
        this.longTimeoutRestTemplate = longTimeoutRestTemplate;
    }

    public ResponseEntity<PipelineRunDTO> startPipelineRun(String artifactType, String artifactUid, String branch,
                                                           Integer docId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        PipelineRunRequestDTO body = PipelineRunRequestDTO.builder()
                .artifactType(artifactType)
                .artifactUid(artifactUid)
                .source(MANUAL_SOURCE)
                .branch(branch)
                .payload(new PipelineRunRequestDTO.Payload(docId))
                .build();
        String url = stagingUrl + "/api/v1/pipeline-runs";
        log.info("POST staging: {}, artifactType={}, artifactUid={}, branch={}, docId={}",
                url, artifactType, artifactUid, branch, docId);
        try {
            return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), PipelineRunDTO.class);
        } catch (RestClientResponseException e) {
            log.error("❌ POST staging {} вернул {}: {}", url, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    public PipelineRunStatusDTO waitPipelineRunTerminal(Long runId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = stagingUrl + "/api/v1/pipeline-runs/" + runId + "/status?waitFor=terminal";
        log.info("GET staging: {}", url);
        PipelineRunStatusDTO response = longTimeoutRestTemplate.exchange(url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                PipelineRunStatusDTO.class).getBody();
        if (response == null || response.getStatus() == null) {
            throw new IllegalStateException("staging вернул пустой статус прогона " + runId);
        }
        return response;
    }
}
