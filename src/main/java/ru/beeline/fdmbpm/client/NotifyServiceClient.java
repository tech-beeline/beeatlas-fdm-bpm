package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.DocIdDTO;

@Slf4j
@Service
public class NotifyServiceClient {

    private final RestTemplate restTemplate;

    private final String notifyServiceUrl;

    public NotifyServiceClient(@Value("${integration.notification-server-url}") String notifyServiceUrl,
                               RestTemplate restTemplate) {
        this.notifyServiceUrl = notifyServiceUrl;
        this.restTemplate = restTemplate;
    }

    public DocIdDTO postExportNotify(Integer docId, String entityType, Integer userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("user-id", String.valueOf(userId));
            headers.setContentType(MediaType.APPLICATION_JSON);

            return restTemplate.exchange(notifyServiceUrl + "/api/v1/notify/business-event/" + entityType + "/" + docId,
                    HttpMethod.POST, new HttpEntity<>(headers), DocIdDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
