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
import ru.beeline.fdmbpm.dto.DocIdDTO;
import ru.beeline.fdmbpm.dto.techradar.ProductDTO;
import ru.beeline.fdmbpm.dto.techradar.ProcessDTO;

import java.util.List;


@Slf4j
@Service
public class TechradarClient {
    RestTemplate restTemplate;
    private final String techradarServerUrl;

    public TechradarClient(@Value("${integration.techradar-server-url}") String techradarServerUrl,
                           RestTemplate restTemplate) {
        this.techradarServerUrl = techradarServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<ProductDTO> getTech() {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(techradarServerUrl + "/api/v1/tech/product-tech",
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<ProductDTO>>() {
                    }).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<ProcessDTO> getProcesses() {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(techradarServerUrl + "/api/v1/processes",
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<ProcessDTO>>() {
                    }).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public DocIdDTO postExportTech(Integer docId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return restTemplate.exchange(techradarServerUrl + "/api/v1/tech/export/" + docId,
                    HttpMethod.POST, new HttpEntity<>(headers), DocIdDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
