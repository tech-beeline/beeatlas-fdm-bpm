/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.dashboard.DashboardCapabilityDTO;
import ru.beeline.fdmbpm.dto.dashboard.DashboardProductsDTO;
import ru.beeline.fdmbpm.dto.dashboard.DashboardTechCapabilityDTO;

import java.util.List;

@Slf4j
@Service
public class DashboardClient {

    RestTemplate restTemplate;
    private final String dashboardServerUrl;

    public DashboardClient(@Value("${integration.dashboard-server-url}") String dashboardServerUrl,
                           RestTemplate restTemplate) {
        this.dashboardServerUrl = dashboardServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<DashboardCapabilityDTO> getCapabilities() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<DashboardCapabilityDTO> result = restTemplate.exchange(dashboardServerUrl + "/api/capabilities",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<DashboardCapabilityDTO>>() {
                            })
                    .getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("dashboard's Capabilities aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error("ERROR SEND TO /api/capabilities " + e.getMessage());
        }
        return null;
    }

    public List<DashboardTechCapabilityDTO> getTechCapabilities() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<DashboardTechCapabilityDTO> result = restTemplate.exchange(dashboardServerUrl + "/api/tech-capabilities",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<DashboardTechCapabilityDTO>>() {
                            })
                    .getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("dashboard's TechCapabilities aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error("ERROR SEND TO /api/tech-capabilities " + e.getMessage());
        }
        return null;
    }

    public List<DashboardProductsDTO> getProducts() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<DashboardProductsDTO> result = restTemplate.exchange(dashboardServerUrl + "/api/v4/systems?level=systems",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<DashboardProductsDTO>>() {
                            })
                    .getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("dashboard's Products aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error("ERROR SEND TO /api/v4/systems " + e.getMessage());
        }
        return null;
    }
}
