package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.ViewPlatformClient;
import ru.beeline.fdmbpm.dto.TokenDTO;
import ru.beeline.fdmbpm.dto.product.ProductDTO;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component("PublicToObservabilityPlatformDelegate")
public class PublicToObservabilityPlatformDelegate implements JavaDelegate {

    @Value("${app.auth_view_platform:true}")
    private boolean authViewPlatform;

    @Value("${integration.observability-login}")
    private String observabilityLogin;

    @Value("${integration.observability-pass}")
    private String observabilityPass;

    @Autowired
    private ViewPlatformClient viewPlatformClient;

    @Autowired
    ProductClient productClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Публикация в платформу наблюдаемости");
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        ProductDTO productDTO = productClient.getProductByCmdb(cmdb).getBody();
        Integer modelId;
        log.info("Получили продук по cmdb: {}", cmdb);
        if (productDTO != null && productDTO.getStructurizrApiUrl() != null) {
            modelId = getModelId(productDTO.getStructurizrApiUrl());
        } else {
            throw new RuntimeException("❌ product или StructurizrApiUrl = null");
        }
        if (authViewPlatform) {
            log.info("auth_view_platform: {}. POST запрос токена в Платформу наблюдаемости", authViewPlatform);
            String logPass = encode(observabilityLogin + ":" + observabilityPass);
            TokenDTO tokenDTO = viewPlatformClient.getToken(logPass);
            String token = tokenDTO.getToken();
            log.info("Получен токен: {}", token.substring(token.length() - 4));
            viewPlatformClient.postStructurizrModel(token, modelId);
        } else {
            log.info("auth_view_platform: {}. POST запрос в Платформу наблюдаемости", authViewPlatform);
            viewPlatformClient.postStructurizrModel(null, modelId);
        }
        log.info("Шаг: Публикация в платформу наблюдаемости: завершен! ");
    }

    private String encode(String text) {
        return Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private Integer getModelId(String structurizrApiUrl) {
        if (structurizrApiUrl == null || structurizrApiUrl.isEmpty()) {
            throw new IllegalArgumentException("❌ URL не может быть null или пустым");
        }
        int lastSlashIndex = structurizrApiUrl.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            throw new IllegalArgumentException("❌ URL должен содержать /");
        }
        String idStr = structurizrApiUrl.substring(lastSlashIndex + 1);
        return Integer.parseInt(idStr);
    }
}
