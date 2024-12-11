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
import ru.beeline.fdmbpm.dto.DashboardCapabilityDTO;
import ru.beeline.fdmbpm.dto.DashboardProductsDTO;
import ru.beeline.fdmbpm.dto.DashboardTechCapabilityDTO;

import java.util.List;

@Slf4j
@Service
public class DashboardClient {

    RestTemplate restTemplate;
    private final String capabilityServerUrl;

    public DashboardClient(@Value("${integration.dashboard-server-url}") String capabilityServerUrl,
                           RestTemplate restTemplate) {
        this.capabilityServerUrl = capabilityServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<DashboardCapabilityDTO> getCapabilities() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<DashboardCapabilityDTO> result = restTemplate.exchange(capabilityServerUrl + "/api/capabilities", HttpMethod.GET, entity, new ParameterizedTypeReference<List<DashboardCapabilityDTO>>() {
            }).getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("dashboard's Capabilities aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<DashboardTechCapabilityDTO> getTechCapabilities() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<DashboardTechCapabilityDTO> result = restTemplate.exchange(capabilityServerUrl + "/api/tech-capabilities", HttpMethod.GET, entity, new ParameterizedTypeReference<List<DashboardTechCapabilityDTO>>() {
            }).getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("dashboard's TechCapabilities aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<DashboardProductsDTO> getProducts() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<DashboardProductsDTO> result = restTemplate.exchange(
                    capabilityServerUrl + "/api/v4/systems?level=systems",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<DashboardProductsDTO>>() {
                    }).getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("dashboard's Products aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
