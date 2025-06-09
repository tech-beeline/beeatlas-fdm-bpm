package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationParticipantDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.UserProfileDTO;
import ru.beeline.fdmlib.dto.capability.BusinessCapabilityDTO;

import java.util.List;

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
        }  catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    public List<ApplicationParticipantDTO> getUsersInfo(List<Integer> ids) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Integer>> entity = new HttpEntity<>(ids, headers);

            return restTemplate.exchange(userServerUrl + "/api/v1/user/list",
                    HttpMethod.POST, entity, new ParameterizedTypeReference<List<ApplicationParticipantDTO>>() {}).getBody();
        }  catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
