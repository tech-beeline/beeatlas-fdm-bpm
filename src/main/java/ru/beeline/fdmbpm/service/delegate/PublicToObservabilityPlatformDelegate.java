/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.ViewPlatformClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.TokenDTO;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component("PublicToObservabilityPlatformDelegate")
public class PublicToObservabilityPlatformDelegate extends StatusLogic implements JavaDelegate {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Публикация в платформу наблюдаемости");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        log.info("process_id: {}", processId);
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        log.info("camundaProcess : {}", camundaProcess);
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        log.info("typeProcess : {}", typeProcess);
        try {
            ProductDTO productDTO = productClient.getProductByCmdb(cmdb).getBody();
            Long modelId;
            log.info("Получили продукт по cmdb: {}", cmdb);
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
            saveAlias(processId, "vpcrt", typeProcess);
            log.info("Шаг: Публикация в платформу наблюдаемости: завершен! ");
        } catch (Exception e) {
            log.error("Ошибка при Публикация в платформу наблюдаемости. Создание записи с ошибкой", e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tt.execute(status -> {
                StatusProcess statusProcess =
                        statusProcessRepository.findByAliasAndTypeProcessId("vperr", typeProcess.getId());
                if (camundaProcessStatusRepository.findByCamundaProcessIdAndStatusProcessId(processId,
                        statusProcess.getId()).isEmpty()) {
                    saveAlias(processId, "vperr", typeProcess);
                }
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Публикация в платформу наблюдаемости");
        }
    }

    private String encode(String text) {
        return Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private Long getModelId(String structurizrApiUrl) {
        if (structurizrApiUrl == null || structurizrApiUrl.isEmpty()) {
            throw new IllegalArgumentException("❌ URL не может быть null или пустым");
        }
        int lastSlashIndex = structurizrApiUrl.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            throw new IllegalArgumentException("❌ URL должен содержать /");
        }
        String idStr = structurizrApiUrl.substring(lastSlashIndex + 1);
        return Long.parseLong(idStr);
    }
}
