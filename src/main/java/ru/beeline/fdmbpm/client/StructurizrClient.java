/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
public class StructurizrClient {

    RestTemplate longTimeoutRestTemplate;

    public StructurizrClient(RestTemplate longTimeoutRestTemplate) {

        this.longTimeoutRestTemplate = longTimeoutRestTemplate;
    }

    public String getDocs(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.error("Structurizr url:" + url + "/json");
            String result = longTimeoutRestTemplate.exchange(url + "/json",
                    HttpMethod.GET, entity,
                    String.class).getBody();
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
