package ru.beeline.fdmbpm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.cmdb.PostProductRequest;
import ru.beeline.fdmbpm.dto.mapic.MethodDTO;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmlib.dto.product.DiscoveredInterfaceDTO;
import ru.beeline.fdmlib.dto.product.PublishedApiDTO;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class ProductClient {

    RestTemplate restTemplate;
    RestTemplate longTimeoutRestTemplate;
    private final String productServerUrl;

    public ProductClient(@Value("${integration.products-server-url}") String productServerUrl,
                         RestTemplate restTemplate,
                         @Qualifier("longTimeoutRestTemplate") RestTemplate longTimeoutRestTemplate) {
        this.productServerUrl = productServerUrl;
        this.restTemplate = restTemplate;
        this.longTimeoutRestTemplate = longTimeoutRestTemplate;
    }

    public void deleteRelation(Integer techId, Integer productId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            restTemplate.exchange(productServerUrl + "/api/v1/product-tech-relation/" + techId + "/" + productId,
                    HttpMethod.DELETE,
                    new HttpEntity(headers),
                    Object.class).getBody();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void postProductCMDB(String product, PostProductRequest postProductRequest) {
        try {
            String json = new ObjectMapper().writeValueAsString(postProductRequest);
            log.info("Sending JSON to CMDB. Size: {} bytes", json.length());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PostProductRequest> requestEntity = new HttpEntity<>(postProductRequest, headers);
            String url = UriComponentsBuilder.fromHttpUrl(productServerUrl + "/api/v1/infra")
                    .queryParam("product", product).toUriString();
            log.info("POST: {}", url);
            ResponseEntity<Void> response = longTimeoutRestTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    Void.class);
            if (response.getStatusCode() != HttpStatus.CREATED) {
                log.error("Unexpected status code from CMDB: {}", response.getStatusCode());
                throw new RuntimeException("Failed to post product. HTTP Status: " + response.getStatusCode());
            }
            log.info("Product posted successfully with status 201.");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error while posting product '{}'. Status: {}, Body: {}", product, e.getStatusCode(),
                    e.getResponseBodyAsString(), e);
            throw new RuntimeException("Error during post request", e);
        } catch (Exception e) {
            log.error("General error while posting product '{}', Message: {}", product, e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

    public List<String> getProductMnemonics() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<String> result = restTemplate.exchange(productServerUrl + "/api/v1/products/mnemonic",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<String>>() {
                    }).getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("Mnemonics aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<PublishedApiDTO> getInterfaces(String cmdb) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<PublishedApiDTO> result = restTemplate.exchange(productServerUrl + "/api/v1/mapic/product/" + cmdb + "/published-api",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<PublishedApiDTO>>() {
                            })
                    .getBody();
            if (result == null || result.size() == 0) {
                throw new RuntimeException("Interfaces aren't received");
            }
            return result;
        } catch (Exception e) {
            log.error("Interfaces aren't received" + e.getMessage());
        }
        return new ArrayList<>();
    }

    public ProductDTO getProduct(String product) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(productServerUrl + "/api/v1/product/" + product,
                    HttpMethod.GET,
                    entity,
                    ProductDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public DiscoveredInterfaceDTO getInterfaceOperations(Integer apiId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(productServerUrl + "/api/v1/discovered-interface?external-id=" + apiId,
                    HttpMethod.GET,
                    entity,
                    DiscoveredInterfaceDTO.class).getBody();

        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Ошибка запроса к product-service: {}", e.getMessage());
            throw new ValidationException(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public void updateInterfaceOperations(List<MethodDTO> methods, Integer id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<MethodDTO>> requestEntity = new HttpEntity<>(methods, headers);
            log.info("response from product:" + restTemplate.exchange(productServerUrl + "/api/v1/discovered-interface/" + id + "/operations",
                    HttpMethod.PUT,
                    requestEntity,
                    String.class));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public void updateInterface(DiscoveredInterfaceDTO body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            List<DiscoveredInterfaceDTO> discoveredInterfaceDTOS = new ArrayList<>();
            discoveredInterfaceDTOS.add(body);
            HttpEntity<List<DiscoveredInterfaceDTO>> requestEntity = new HttpEntity<>(discoveredInterfaceDTOS, headers);
            log.info("response from productService:" + restTemplate.exchange(productServerUrl + "/api/v1/discovered-interfaces",
                    HttpMethod.PUT,
                    requestEntity,
                    String.class));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public ProductDTO getProductInfoByCmdb(String cmdb) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ProductDTO result = restTemplate.exchange(productServerUrl + "/api/v1/product/" + cmdb,
                    HttpMethod.GET,
                    entity,
                    ProductDTO.class).getBody();
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public String getMapicSpec(Integer apiId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(productServerUrl + "/api/v1/mapic/spec/" + apiId,
                    HttpMethod.GET,
                    entity,
                    String.class).getBody();
        } catch (Exception e) {
            log.error("Error get specification from mapic " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
