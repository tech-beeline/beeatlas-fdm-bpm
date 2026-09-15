/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.smartkts.StructurizrObjectDTO;

@Slf4j
@Service
public class SmartKtsClient {

    private static final String GENERATE_PATH = "/generate_by_structirizr_id_with_link";

    private final RestTemplate restTemplate;
    private final String smartKtsUrl;

    @Autowired
    public SmartKtsClient(@Value("${integration.smartkts.url:}") String smartKtsUrl,
                          @Value("${integration.smartkts.connect-timeout-ms:5000}") int connectTimeoutMs,
                          @Value("${integration.smartkts.read-timeout-ms:300000}") int readTimeoutMs,
                          RestTemplateBuilder builder) {
        this(smartKtsUrl, buildRestTemplate(builder, connectTimeoutMs, readTimeoutMs));
    }

    SmartKtsClient(String smartKtsUrl, RestTemplate restTemplate) {
        this.smartKtsUrl = smartKtsUrl == null ? "" : smartKtsUrl.trim().replaceAll("/+$", "");
        this.restTemplate = restTemplate;
    }

    public StructurizrObjectDTO generateKtsPassport(String structurizrId, String bwikiKtsLink) {
        if (smartKtsUrl.isEmpty()) {
            throw new IllegalStateException("Не задан адрес SmartKTS: integration.smartkts.url");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        StructurizrObjectDTO request = StructurizrObjectDTO.builder()
                .structurizrId(structurizrId)
                .bwikiKtsLink(bwikiKtsLink)
                .build();
        try {
            ResponseEntity<StructurizrObjectDTO> response = restTemplate.exchange(smartKtsUrl + GENERATE_PATH,
                    HttpMethod.POST, new HttpEntity<>(request, headers), StructurizrObjectDTO.class);
            log.info("SmartKTS: HTTP {}, body: {}", response.getStatusCode().value(), response.getBody());
            return response.getBody();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("SmartKTS вернул HTTP " + e.getStatusCode().value() + ": "
                    + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("SmartKTS недоступен: " + e.getMessage(), e);
        }
    }

    private static RestTemplate buildRestTemplate(RestTemplateBuilder builder, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return builder.requestFactory(() -> factory).build();
    }
}
