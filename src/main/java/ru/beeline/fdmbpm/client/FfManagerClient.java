package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import ru.beeline.fdmbpm.dto.ffmanager.PassedFfRequestDTO;
import ru.beeline.fdmbpm.dto.ffmanager.PostFfManagerDTO;
import ru.beeline.fdmbpm.dto.ffmanager.ProductActualResultsDTO;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        postFfManager(body, docId, processId, null);
    }

    public void postFfManager(PostFfManagerDTO body, Integer docId, Integer processId, String branch) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PostFfManagerDTO> entity = new HttpEntity<>(body, headers);
            UriComponentsBuilder urlBuilder = UriComponentsBuilder
                    .fromHttpUrl(ffManagerUrl + "/api/v1/run-all")
                    .queryParam("docId", docId)
                    .queryParam("source_type", "pipeline")
                    .queryParam("source_id", processId);
            if (branch != null && !branch.isBlank()) {
                urlBuilder.queryParam("branch", branch.trim());
            }
            String url = urlBuilder.toUriString();
            log.info("Отправка POST запроса в FF Manager: {}, app: {}, branch: {}",
                    url, body.getApp(), blankToNull(branch));
            ResponseEntity<Object> response = longTimeoutRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Object.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("ℹ️ [ff-manager] run-all OK | status={}, docId={}, app={}, source_id={}, branch={}",
                        response.getStatusCode(), docId, body.getApp(), processId, blankToNull(branch));
            } else {
                log.warn("ℹ️ [ff-manager] run-all non-2xx (не пробрасывается в Camunda) | status={}, docId={}, app={}, branch={}",
                        response.getStatusCode(), docId, body.getApp(), blankToNull(branch));
            }
        } catch (Exception e) {
            log.error("ℹ️ [ff-manager] run-all FAILED (исключение глотается) | docId={}, app={}, source_id={}, branch={}: {}",
                    docId, body.getApp(), processId, blankToNull(branch), e.getMessage(), e);
        }
    }

    public void runFitnessFunction(String code, String alias, Integer docId, Integer sourceId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String encodedCode = UriUtils.encodePathSegment(code, StandardCharsets.UTF_8);
            String url = UriComponentsBuilder.fromHttpUrl(ffManagerUrl + "/api/v1/run/" + encodedCode)
                    .queryParam("docId", docId)
                    .queryParam("source_type", "recalc")
                    .queryParam("source_id", sourceId)
                    .toUriString();
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("app", alias), headers);
            log.info("POST FF Manager run: url={}, app={}", url, alias);
            ResponseEntity<Object> response = longTimeoutRestTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("FF Manager run non-2xx | status={}, code={}, app={}, docId={}",
                        response.getStatusCode(), code, alias, docId);
            }
        } catch (Exception e) {
            log.error("FF Manager run FAILED | code={}, app={}, docId={}, source_id={}: {}",
                    code, alias, docId, sourceId, e.getMessage(), e);
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
