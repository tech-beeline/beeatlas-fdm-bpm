/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.ModelIdDTO;
import ru.beeline.fdmbpm.dto.TokenDTO;

@Slf4j
@Service
public class ViewPlatformClient {

    RestTemplate restTemplate;
    private final String viewPlatformUrl;

    public ViewPlatformClient(@Value("${integration.view-platform-server-url}") String viewPlatformUrl,
                              RestTemplate restTemplate) {
        this.viewPlatformUrl = viewPlatformUrl;
        this.restTemplate = restTemplate;
    }

    public TokenDTO getToken(String logPass) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + logPass);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(viewPlatformUrl + "/api/token",
                    HttpMethod.POST, entity, new ParameterizedTypeReference<TokenDTO>() {
                    }).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public void postStructurizrModel(String token, Long modelId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null) {
                headers.set("Authorization", "Bearer " + token);
            }
            ModelIdDTO modelIdDTO = ModelIdDTO.builder()
                    .modelId(modelId)
                    .build();
            HttpEntity<ModelIdDTO> requestEntity = new HttpEntity<>(modelIdDTO, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    viewPlatformUrl + "/api/structurizrmodel",
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            log.info("Status: {}", response.getStatusCode());
            log.info("Body: {}", response.getBody());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
