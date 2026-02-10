package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.fdmbpm.dto.DocIdDTO;
import ru.beeline.fdmbpm.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmbpm.dto.applicationDTO.AdditionalInfoDTO;
import ru.beeline.fdmbpm.dto.dashboard.DashboardCapabilityDTO;
import ru.beeline.fdmbpm.dto.dashboard.DashboardTechCapabilitiesDTO;
import ru.beeline.fdmbpm.dto.dashboard.DashboardTechCapabilityDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.dto.capability.BusinessCapabilityDTO;
import ru.beeline.fdmbpm.dto.capability.BusinessCapabilityOrderDraftResponseDTO;
import ru.beeline.fdmbpm.dto.capability.TechCapabilityShortDTO;

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
                    HttpMethod.POST,
                    entity,
                    PackageRegistrationResponseDTO.class).getBody();
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
                    HttpMethod.POST,
                    entity,
                    PackageRegistrationResponseDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public PackageRegistrationResponseDTO deleteBusinessCapabilities(List<BusinessCapabilityDTO> list) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            log.info("BusinessCapabilityDTO list size = " + list.size());
            for (BusinessCapabilityDTO dto : list) {
                HttpEntity<String> entity = new HttpEntity<>(headers);
                String url = capabilityServerUrl + "/api/v1/business-capability/" + dto.getCode();
                log.info("dto.getCode = " + dto.getCode() + ". " + url);
                restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public PackageRegistrationResponseDTO deleteTechCapabilities(List<TechCapabilityShortDTO> list) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            for (TechCapabilityShortDTO dto : list) {
                HttpEntity<String> entity = new HttpEntity<>(headers);
                String url = capabilityServerUrl + "/api/v1/tech-capabilities/" + dto.getCode();
                restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public void calculateTotalTechCapabiltiesCount() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<List<DashboardCapabilityDTO>> entity = new HttpEntity<>(null, headers);
            restTemplate.exchange(capabilityServerUrl + "/api/v1/calculate-total-tech-capabilities",
                    HttpMethod.POST,
                    entity,
                    Object.class);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public List<AdditionalInfoDTO> getAdditionalInfoDTO(List<Integer> ids) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<List<Integer>> entity = new HttpEntity<>(ids, headers);
            return restTemplate.exchange(capabilityServerUrl + "/api/v1/business-capability/order/domains",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<AdditionalInfoDTO>>() {
                    }).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public List<BusinessCapabilityDTO> getBusinessCapabilities() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<BusinessCapabilityDTO> result = restTemplate.exchange(capabilityServerUrl + "/api/v1/business-capability",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<BusinessCapabilityDTO>>() {
                            })
                    .getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("Business-capability aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<TechCapabilityShortDTO> getTechCapabilities() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<TechCapabilityShortDTO> result = restTemplate.exchange(capabilityServerUrl + "/api/v1/tech-capabilities",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<TechCapabilityShortDTO>>() {
                            })
                    .getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("Tech-capabilities aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public DocIdDTO postExportCapability(Integer docId, String capability) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return restTemplate.exchange(capabilityServerUrl + "/api/v1/export/" + capability + "/" + docId,
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    DocIdDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());

        }
        return null;
    }

    public BusinessCapabilityOrderDraftResponseDTO getBusinessCapabilityOrder(Integer entityId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            BusinessCapabilityOrderDraftResponseDTO result = restTemplate.exchange(capabilityServerUrl + "/api/v1/business-capability/order/" + entityId,
                            HttpMethod.GET, entity, new ParameterizedTypeReference<BusinessCapabilityOrderDraftResponseDTO>() {
                            })
                    .getBody();
            return result;
        } catch (HttpClientErrorException.NotFound e) {
            String message = e.getResponseBodyAsString();
            throw new NotFoundException(message);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}