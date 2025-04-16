package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.DocIdDTO;
import ru.beeline.fdmbpm.dto.cmdb.PostProductRequest;


@Slf4j
@Service
public class ProductClient {

    RestTemplate restTemplate;
    private final String productServerUrl;

    public ProductClient(@Value("${integration.products-server-url}") String productServerUrl, RestTemplate restTemplate) {
        this.productServerUrl = productServerUrl;
        this.restTemplate = restTemplate;
    }

    public void deleteRelation(Integer techId, Integer productId){
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            restTemplate.exchange(productServerUrl + "/api/v1/product-tech-relation/" + techId + "/" + productId,
                    HttpMethod.DELETE, new HttpEntity(headers), Object.class).getBody();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void postProductCMDB(String product, PostProductRequest postProductRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(postProductRequest, headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    productServerUrl + "/api/v1/infra/" + product,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new RuntimeException("Failed to post product. HTTP Status: " + response.getStatusCode());
            }
            log.info("Product posted successfully with status 201.");
        } catch (Exception e) {
            log.error("Error while posting product to CMDB: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }
}
