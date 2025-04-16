package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.cmdb.PostProductRequest;
import ru.beeline.fdmbpm.dto.product.ProductDTO;

import java.util.List;


@Slf4j
@Service
public class StructurizrClient {

    RestTemplate restTemplate;

    public StructurizrClient(RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }

    public String getDocs(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String result = restTemplate.exchange(url + "/json",
                    HttpMethod.GET, entity,
                    String.class).getBody();
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
