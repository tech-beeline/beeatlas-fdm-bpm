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
import ru.beeline.fdmbpm.dto.dashboard.DashboardCapabilityV4DTO;
import ru.beeline.fdmbpm.dto.dashboard.DashboardProductsDTO;
import ru.beeline.fdmbpm.dto.dashboard.DashboardTechCapabilityDTO;
import ru.beeline.fdmlib.dto.capability.BusinessCapabilityOrderDraftResponseDTO;

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
                                                                        new ParameterizedTypeReference<List<DashboardCapabilityDTO>>() {})
                    .getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("dashboard's Capabilities aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public void putCapability(BusinessCapabilityOrderDraftResponseDTO order) {
        try {
            DashboardCapabilityV4DTO dashboardCapabilityV4DTO = DashboardCapabilityV4DTO.builder()
                    .isDomain(false)
                    .name(order.getName())
                    .description(order.getDescription())
                    .author(order.getAuthor())
                    .owner(order.getOwner())
                    .parent(order.getParent() == null ? null : order.getParent().getCode())
                    .self("/api/v4/capabilities/undefined")
                    .build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DashboardCapabilityV4DTO> requestEntity = new HttpEntity<>(dashboardCapabilityV4DTO, headers);
            log.info("response from dashboard:" + restTemplate.exchange(dashboardServerUrl + "/api/capabilities/" + order.getMutable()
                    .getCode(), HttpMethod.PUT, requestEntity, String.class));

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public List<DashboardTechCapabilityDTO> getTechCapabilities() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<DashboardTechCapabilityDTO> result = restTemplate.exchange(dashboardServerUrl + "/api/tech-capabilities",
                                                                            HttpMethod.GET,
                                                                            entity,
                                                                            new ParameterizedTypeReference<List<DashboardTechCapabilityDTO>>() {})
                    .getBody();
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
            List<DashboardProductsDTO> result = restTemplate.exchange(dashboardServerUrl + "/api/v4/systems?level=systems",
                                                                      HttpMethod.GET,
                                                                      entity,
                                                                      new ParameterizedTypeReference<List<DashboardProductsDTO>>() {})
                    .getBody();
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
