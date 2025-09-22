package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class StageMapicClient {

    RestTemplate restTemplate;

    private final String serverUrl;

    public StageMapicClient(@Value("${integration.stage-mapic-server-url}") String serverUrl,
                            RestTemplate restTemplate) {
        this.serverUrl = serverUrl;
        this.restTemplate = restTemplate;
    }

    public String getSpecification(Integer id) {
        try {
            String url = serverUrl + "/api-management/api/v7/api/" + id + "/specification";
            log.info("send request to:" + url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(url,
                                         HttpMethod.GET,
                                         entity,
                                         String.class).getBody();

        } catch (Exception e) {
            log.error("Error get specification from mapic " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

}