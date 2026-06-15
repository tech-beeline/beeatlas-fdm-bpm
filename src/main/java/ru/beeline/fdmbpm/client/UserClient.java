/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationParticipantDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.AuthUserDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.BeeworksUserProductsDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.UserProfileDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.UserShortDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserClient {

    RestTemplate restTemplate;
    private final String userServerUrl;

    public UserClient(@Value("${integration.auth-server-url}") String userServerUrl, RestTemplate restTemplate) {
        this.userServerUrl = userServerUrl;
        this.restTemplate = restTemplate;
    }

    public UserProfileDTO getUserProfile(Integer id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(userServerUrl + "/api/v1/user/" + id,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<UserProfileDTO>() {
                    }).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            String message = extractErrorMessage(e.getResponseBodyAsString());
            throw new NotFoundException(message);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public BeeworksUserProductsDTO getBeeworksUserProducts(String login) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(userServerUrl + "/api/bw/products/" + login,
                    HttpMethod.GET,
                    entity,
                    BeeworksUserProductsDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    public List<ApplicationParticipantDTO> getUsersInfo(List<Integer> ids) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Integer>> entity = new HttpEntity<>(ids, headers);

            return restTemplate.exchange(userServerUrl + "/api/v1/user/list",
                    HttpMethod.POST, entity, new ParameterizedTypeReference<List<ApplicationParticipantDTO>>() {
                    }).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    public List<UserShortDTO> getAllUsers() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            List<AuthUserDTO> users = restTemplate.exchange(userServerUrl + "/api/v1/users",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<AuthUserDTO>>() {
                    }).getBody();

            if (users == null || users.isEmpty()) {
                return Collections.emptyList();
            }

            return users.stream()
                    .map(user -> UserShortDTO.builder()
                            .id(user.getId())
                            .login(user.getLogin())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseBody);
            if (node.has("errorMessage")) {
                return node.get("errorMessage").asText();
            }
            if (node.has("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }
}
