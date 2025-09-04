package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.cmdb.PostProductRequest;


@Slf4j
@Service
public class ArchClient {

    RestTemplate restTemplate;
    private final String archServerUrl;

    public ArchClient(@Value("${integration.arch-server-url}") String archServerUrl, RestTemplate restTemplate) {
        this.archServerUrl = archServerUrl;
        this.restTemplate = restTemplate;
    }

    public void postFitnessFunction(Integer docId, Integer processId) {
        try {
            log.info("try postFitnessFunction for serv:" + archServerUrl + "/api/v1/fitness-function/local/" + docId + "?pipelineId=" + processId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    archServerUrl + "/api/v1/workspace/" + docId + "/fdm",
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error while posting Fitness Function: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

    public void publicToStructurizr(Integer docId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    archServerUrl + "/api/v1/workspace/" + docId,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error while posting Fitness Function: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }
}
