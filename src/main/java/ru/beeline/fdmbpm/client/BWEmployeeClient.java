/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.bw.BWToken;
import ru.beeline.fdmbpm.dto.bw.BwProductDTO;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class BWEmployeeClient {

    @Autowired
    RestTemplate restTemplate;

    private static String accessToken = "";

    @Value("${gwurl}")
    private String gwUrl;

    @Value("${authbasic}")
    private String authBasic;

    @Value("${techuser}")
    private String techUser;

    @Value("${techpassword}")
    private String techPassword;

    private static int attemptCounter = 0;

    public List<BwProductDTO> getProducts() {
        List<BwProductDTO> productsDTO = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);


            productsDTO = restTemplate.exchange(
                    gwUrl + "/appgit/v2/products",
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<BwProductDTO>>() {
                    }).getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error(e.getMessage());
            if (attemptCounter < 3) {
                attemptCounter++;
                log.info("The MAPIC token update attempt: " + attemptCounter);
                updateAccessToken();
                productsDTO = getProducts();
            } else {
                attemptCounter = 0;
                log.error(e.getMessage());
            }

        } catch (Exception e) {
            attemptCounter = 0;
            log.error(e.getMessage());
        }
        attemptCounter = 0;
        return productsDTO;
    }


    public void updateAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + authBasic);

            MultiValueMap<String, String> bodyParamMap = new LinkedMultiValueMap<>();
            bodyParamMap.add("grant_type", "client_credentials");
            bodyParamMap.add("username", techUser);
            bodyParamMap.add("password", techPassword);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(bodyParamMap, headers);

            BWToken token = restTemplate.exchange(
                    gwUrl + "/gw-auth/1.0.0/token",
                    HttpMethod.POST, entity, BWToken.class).getBody();
            if (token != null) {
                log.info("The MAPIC token has been updated");
                accessToken = token.getAccessToken();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

}
