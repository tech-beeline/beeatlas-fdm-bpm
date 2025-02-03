package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.PackageRegistrationRequestDTO;
import ru.beeline.fdmbpm.dto.PackageRegistrationResponseDTO;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Slf4j
@Service
public class PackageClient {
    RestTemplate restTemplate;
    private final String packLoaderServerUrl;

    public PackageClient(@Value("${integration.pack-loader-server-url}") String packLoaderServerUrl,
                         RestTemplate restTemplate) {
        this.packLoaderServerUrl = packLoaderServerUrl;
        this.restTemplate = restTemplate;
    }

    public PackageRegistrationResponseDTO registerPackage(String operation, int dataSize) {
        return registerPackage(operation, dataSize, "");
    }

    public PackageRegistrationResponseDTO registerPackage(String operation, int dataSize, String additionalParam1) {
        return registerPackage(operation, dataSize, additionalParam1, "");
    }

    public PackageRegistrationResponseDTO registerPackage(String operation, int dataSize, String source , String status ) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PackageRegistrationRequestDTO> entity = new HttpEntity<>(PackageRegistrationRequestDTO.builder()
                    .operation(operation)
                    .count(dataSize)
                    .build(),
                    headers);
            String url = packLoaderServerUrl + "/api/v1/package";
            if (source != null && !source.isEmpty()) {
                url += "?source=" + URLEncoder.encode(source, StandardCharsets.UTF_8);
            }
            if (status != null && !status.isEmpty()) {
                url += (url.contains("?") ? "&" : "?") + "status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);
            }
            return restTemplate.exchange(url,
                    HttpMethod.POST, entity, PackageRegistrationResponseDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
