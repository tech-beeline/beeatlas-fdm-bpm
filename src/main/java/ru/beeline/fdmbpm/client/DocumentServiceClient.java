package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class DocumentServiceClient {

    private final RestTemplate restTemplate;

    private final String documentServiceUrl;

    public DocumentServiceClient(@Value("${integration.document-server-url}") String documentServiceUrl,
                                 RestTemplate restTemplate) {
        this.documentServiceUrl = documentServiceUrl;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<byte[]> getDocument(Integer docId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(
                    documentServiceUrl + "/api/v1/documents/" + docId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<byte[]>() {
                    }
            );
        }catch (HttpServerErrorException.ServiceUnavailable e) {
            log.error("Ошибка при загрузке документа: ", e);
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }catch (HttpClientErrorException.NotFound e){
            log.error("Запись с данным id не найдена: ", e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
    }
}
