package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.cmdb.PostProductRequest;
import ru.beeline.fdmbpm.dto.graph.GraphDTO;


@Slf4j
@Service
public class GraphClient {

    RestTemplate restTemplate;
    private final String graphServerUrl;

    public GraphClient(@Value("${integration.graph-server-url}") String graphServerUrl, RestTemplate restTemplate) {
        this.graphServerUrl = graphServerUrl;
        this.restTemplate = restTemplate;
    }

    public GraphDTO getLocalGraph(Integer processId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<GraphDTO> response = restTemplate.exchange(graphServerUrl + "api/v1/local/graph/task/" + processId,
                                                                      HttpMethod.GET,
                                                                      requestEntity,
                                                                      GraphDTO.class);
            if (response.getBody().getStatus().equals("ERROR")) {
                throw new Exception("error");
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("Error while posting product to CMDB: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

    public GraphDTO getGlobalGraph(Integer processId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<GraphDTO> response = restTemplate.exchange(graphServerUrl + "api/v1/global/graph/task/" + processId,
                                                                      HttpMethod.GET,
                                                                      requestEntity,
                                                                      GraphDTO.class);
            if (response.getBody().getStatus().equals("ERROR")) {
                throw new Exception("error");
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("Error while posting product to CMDB: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

}
