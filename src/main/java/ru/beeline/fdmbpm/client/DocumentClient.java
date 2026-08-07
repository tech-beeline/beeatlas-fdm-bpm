/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.DocIdDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class DocumentClient {

    private final RestTemplate restTemplate;

    private final String documentServiceUrl;

    public DocumentClient(@Value("${integration.document-server-url}") String documentServiceUrl,
                          RestTemplate restTemplate) {
        this.documentServiceUrl = documentServiceUrl;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<byte[]> getDocument(Integer docId) {
        log.info("ℹ️ Запрос к сервису документов:" + documentServiceUrl + "/api/v1/documents/" + docId);
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
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            log.error("Ошибка при загрузке документа: ", e);
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Запись с данным id не найдена: ", e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
    }

    public DocIdDTO postDocument(String excelFile) {
        try {
            Path tempFile = Files.createTempFile("upload-", ".json");
            Files.write(tempFile, excelFile.getBytes(StandardCharsets.UTF_8));
            FileSystemResource resource = new FileSystemResource(tempFile.toFile());
            String url = documentServiceUrl + "/api/v1/documents/workspace/json?isPublic=true";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "data from structurizr");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            log.info("url: {}", url);
            ResponseEntity<DocIdDTO> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, DocIdDTO.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("File uploaded successfully");
            } else {
                log.error("Failed to upload file: {}", response.getStatusCode());
            }
            Files.deleteIfExists(tempFile);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException(e.getMessage());
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException(e.getMessage());
        } catch (RestClientException e) {
            log.error("Error while uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Error while uploading file");
        } catch (IOException e) {
            log.error("Error while uploading file: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void deleteOldDocuments() {
        log.info("ℹ️ Запрос к сервису документов DELETE:" + documentServiceUrl + "/api/v1/documents");
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    documentServiceUrl + "/api/v1/documents",
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Удаление успешно");
            }
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw e;
        }
    }
}
