package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.DashboardCapabilityDTO;
import ru.beeline.fdmbpm.dto.DashboardTechCapabilitiesDTO;
import ru.beeline.fdmbpm.dto.DashboardTechCapabilityDTO;
import ru.beeline.fdmbpm.dto.PackageRegistrationResponseDTO;

import java.util.List;

@Slf4j
@Service
public class CapabilityClient {

    RestTemplate restTemplate;

    private final String capabilityServerUrl;

    public CapabilityClient(@Value("${integration.capability-server-url}") String capabilityServerUrl,
                            RestTemplate restTemplate) {
        this.capabilityServerUrl = capabilityServerUrl;
        this.restTemplate = restTemplate;
    }

    public PackageRegistrationResponseDTO postTechCapabilities(DashboardTechCapabilitiesDTO body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<List<DashboardTechCapabilityDTO>> entity = new HttpEntity<>(body.getList(), headers);
            return restTemplate.exchange(capabilityServerUrl + "/api/v1/package-tech-capabilities",
                    HttpMethod.POST, entity, PackageRegistrationResponseDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public PackageRegistrationResponseDTO postBusinessCapabilities(List<DashboardCapabilityDTO> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<List<DashboardCapabilityDTO>> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(capabilityServerUrl + "/api/v1/package-business-capabilities",
                    HttpMethod.POST, entity, PackageRegistrationResponseDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public void сalculatePrivateTechCapabiltiesCount(int entityId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<List<DashboardCapabilityDTO>> entity = new HttpEntity<>(null, headers);
            restTemplate.exchange(capabilityServerUrl + "/api/v1/сalculate-private-tech-capabilties/" + entityId,
                    HttpMethod.POST, entity, Object.class);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void сalculateTotalTechCapabiltiesCount() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<List<DashboardCapabilityDTO>> entity = new HttpEntity<>(null, headers);
            restTemplate.exchange(capabilityServerUrl + "/api/v1/сalculate-total-tech-capabilties",
                    HttpMethod.POST, entity, Object.class);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
