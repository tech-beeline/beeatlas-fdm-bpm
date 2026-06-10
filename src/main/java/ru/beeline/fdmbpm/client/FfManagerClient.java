package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.ffmanager.PostFfManagerDTO;
import ru.beeline.fdmbpm.dto.ffmanager.ProductActualResultsDTO;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;

import java.util.ArrayList;
import java.util.List;

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
                log.info("Успешная отправка в FF Manager. Статус: {}, docId: {}, app: {}",
                        response.getStatusCode(), docId, body.getApp());
            } else {
                log.warn("FF Manager вернул ошибку. Статус: {}, docId: {}, app: {}",
                        response.getStatusCode(), docId, body.getApp());
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке запроса в FF Manager. docId: {}, app: {}",
                    docId, body.getApp(), e);
        }
    }

    public List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> getMergedActualResultsForNfr(String productCode) {
        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> merged = new ArrayList<>();
        merged.addAll(mapActualResults(getProductActualResults(productCode, false)));
        merged.addAll(mapActualResults(getProductActualResults(productCode, true)));
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
