package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.ffmanager.PassedFfRequestDTO;
import ru.beeline.fdmbpm.dto.ffmanager.PostFfManagerDTO;
import ru.beeline.fdmbpm.dto.ffmanager.ProductActualResultsDTO;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;

@Slf4j
@Service
public class FfManagerClient {

    private final RestTemplate longTimeoutRestTemplate;
    private final String ffManagerUrl;

    public FfManagerClient(@Value("${integration.ff-manager-url}") String ffManagerUrl,
                           @Qualifier("longTimeoutRestTemplate") RestTemplate longTimeoutRestTemplate) {
        this.ffManagerUrl = ffManagerUrl;
        this.longTimeoutRestTemplate = longTimeoutRestTemplate;
    }

    public void postFfManager(PostFfManagerDTO body, Integer docId, Integer processId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PostFfManagerDTO> entity = new HttpEntity<>(body, headers);
            String url = ffManagerUrl + "/api/v1/run-all?docId=" + docId + "&source_type=pipeline&" +
                    "source_id=" + processId;
            log.info("Отправка POST запроса в FF Manager: {}, app: {}", url, body.getApp());
            ResponseEntity<Object> response = longTimeoutRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Object.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("ℹ️ [ff-manager] run-all OK | status={}, docId={}, app={}, source_id={}",
                        response.getStatusCode(), docId, body.getApp(), processId);
            } else {
                log.warn("ℹ️ [ff-manager] run-all non-2xx (не пробрасывается в Camunda) | status={}, docId={}, app={}",
                        response.getStatusCode(), docId, body.getApp());
            }
        } catch (Exception e) {
            log.error("ℹ️ [ff-manager] run-all FAILED (исключение глотается) | docId={}, app={}, source_id={}: {}",
                    docId, body.getApp(), processId, e.getMessage(), e);
        }
    }

    public List<String> getProductsPassedFf(List<String> rules) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PassedFfRequestDTO> entity = new HttpEntity<>(new PassedFfRequestDTO(rules), headers);
            String url = ffManagerUrl + "/api/v1/products/passed-ff";
            log.info("POST FF Manager passed-ff: url={}, rules={}", url, rules);
            ResponseEntity<List<String>> response = longTimeoutRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            List<String> result = response.getBody();
            log.info("FF Manager passed-ff: получено {} продуктов", result != null ? result.size() : 0);
            return result != null ? result : List.of();
        } catch (Exception e) {
            log.error("Ошибка при вызове FF Manager passed-ff: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> getMergedActualResultsForNfr(String productCode) {
        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> merged = new ArrayList<>();
        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> main =
                mapActualResults(getProductActualResults(productCode, false));
        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> auxiliary =
                mapActualResults(getProductActualResults(productCode, true));
        merged.addAll(main);
        merged.addAll(auxiliary);
        log.info("ℹ️ [ff-manager] merged actual-results for {}: main={}, auxiliary={}, total={}",
                productCode, main.size(), auxiliary.size(), merged.size());
        return merged;
    }

    private ProductActualResultsDTO getProductActualResults(String productCode, boolean auxiliary) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = ffManagerUrl + "/api/v1/product/" + productCode + "/actual-results"
                    + (auxiliary ? "?auxiliary=true" : "");
            log.info("GET FF Manager actual-results: {}, product: {}", url, productCode);
            return longTimeoutRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProductActualResultsDTO.class
            ).getBody();
        } catch (Exception e) {
            log.error("GET FF Manager actual-results for product {} (auxiliary={}) failed: {}",
                    productCode, auxiliary, e.getMessage());
            return null;
        }
    }

    private static List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> mapActualResults(
            ProductActualResultsDTO actualResults) {
        if (actualResults == null || actualResults.getResults() == null) {
            return List.of();
        }
        return actualResults.getResults().stream()
                .filter(item -> item.getFfCode() != null)
                .map(item -> AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO.builder()
                        .code(item.getFfCode())
                        .isCheck(item.getIsCheck())
                        .build())
                .toList();
    }
}
