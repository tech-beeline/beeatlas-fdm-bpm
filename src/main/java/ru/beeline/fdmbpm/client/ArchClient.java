/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.cmdb.PostProductRequest;
import ru.beeline.fdmbpm.exception.ExternalServiceException;


@Slf4j
@Service
public class ArchClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    RestTemplate restTemplate;
    RestTemplate longTimeoutRestTemplate;
    private final String archServerUrl;

    public ArchClient(@Value("${integration.arch-server-url}") String archServerUrl,
                       RestTemplate restTemplate,
                       @Qualifier("longTimeoutRestTemplate") RestTemplate longTimeoutRestTemplate) {
        this.archServerUrl = archServerUrl;
        this.restTemplate = restTemplate;
        this.longTimeoutRestTemplate = longTimeoutRestTemplate;
    }

    public void postFitnessFunction(Integer docId, Integer processId) {
        try {
            log.info("try postFitnessFunction for serv:" + archServerUrl + "/api/v1/fitness-function/local/" + docId + "?pipelineId=" + processId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = longTimeoutRestTemplate.exchange(
                    archServerUrl + "/api/v1/fitness-function/local/" + docId + "?pipelineId=" + processId,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error while posting Fitness Function 1: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

    public void publicFdm(Integer docId) {
        postWorkspace(archServerUrl + "/api/v1/workspace/" + docId + "/fdm", "публикации в ФДМ");
    }

    public void publicToStructurizr(Integer docId) {
        postWorkspace(archServerUrl + "/api/v1/workspace/" + docId, "публикации в Structurizr");
    }

    /**
     * Ошибку structurizr-backend пробрасывает как {@link ExternalServiceException} с HTTP-кодом
     * и полем {@code details} из тела ответа.
     */
    private void postWorkspace(String url, String action) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            longTimeoutRestTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
        } catch (RestClientResponseException e) {
            log.error("Ошибка {}: HTTP {}, тело ответа: {}", action, e.getStatusCode().value(),
                    e.getResponseBodyAsString(), e);
            throw new ExternalServiceException("Ошибка " + action + ": HTTP " + e.getStatusCode().value(),
                    e.getStatusCode().value(), extractDetails(e.getResponseBodyAsString()), e);
        } catch (Exception e) {
            log.error("Ошибка {}: {}", action, e.getMessage(), e);
            throw new ExternalServiceException("Ошибка " + action + ": " + e.getMessage(), null, null, e);
        }
    }

    static String extractDetails(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode details = root.has("details") ? root.get("details") : root.get("detail");
            return details != null && details.isTextual() ? details.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
