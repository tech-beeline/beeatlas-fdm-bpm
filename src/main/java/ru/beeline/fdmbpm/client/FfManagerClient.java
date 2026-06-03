package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.ffmanager.PostFfManagerDTO;

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
}
