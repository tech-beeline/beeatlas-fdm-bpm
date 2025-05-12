package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.cmdb.PostProductRequest;

import java.util.List;


@Slf4j
@Service
public class GraphClient {

    RestTemplate restTemplate;
    private final String graphServerUrl;

    public GraphClient(@Value("${integration.graph-server-url}") String graphServerUrl, RestTemplate restTemplate) {
        this.graphServerUrl = graphServerUrl;
        this.restTemplate = restTemplate;
    }

    public void postLocalGraph(Integer docId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    graphServerUrl + "/api/v1/graph/local/" + docId,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error while posting product to CMDB: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

    public void postGlobalGraph(Integer docId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    graphServerUrl + "/api/v1/graph/" + docId,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error while posting product to CMDB: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

}
