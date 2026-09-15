/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.SmartKtsClient;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.dto.smartkts.StructurizrObjectDTO;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component("GenerateKtsPassportDelegate")
public class GenerateKtsPassportDelegate extends StatusLogic implements JavaDelegate {

    static final String ERROR_VARIABLE = "error";
    static final String KTS_GENERATED_VARIABLE = "ktsGenerated";
    static final String BWIKI_KTS_LINK_VARIABLE = "bwiki_kts_link";
    static final String STATUS_DONE = "ktscrt";
    static final String STATUS_ERROR = "ktserr";

    @Value("${integration.smartkts.enabled:false}")
    boolean smartKtsEnabled;

    @Autowired
    ProductClient productClient;

    @Autowired
    SmartKtsClient smartKtsClient;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Integer processId = (Integer) execution.getVariable("process_id");
        String cmdb = (String) execution.getVariable("cmdb");
        Object docId = execution.getVariable("docId");
        log.info("Шаг: Генерация паспорта КТС в SmartKTS: cmdb={}, process_id={}, docId={}", cmdb, processId, docId);
        if (!smartKtsEnabled) {
            log.info("Шаг пропущен: integration.smartkts.enabled=false (cmdb={}, process_id={}, docId={})",
                    cmdb, processId, docId);
            return;
        }
        TypeProcess typeProcess = typeProcessRepository.findByAlias("Datapipe");
        String bwikiKtsLink = bwikiKtsLink(execution);
        String structurizrId = null;
        try {
            structurizrId = resolveStructurizrId(cmdb);
            log.info("SmartKTS: запуск генерации паспорта КТС: cmdb={}, process_id={}, docId={}, structurizr_id={}, "
                    + "bwiki_kts_link={}", cmdb, processId, docId, structurizrId, bwikiKtsLink);
            StructurizrObjectDTO response = smartKtsClient.generateKtsPassport(structurizrId, bwikiKtsLink);
            log.info("SmartKTS: паспорт КТС сгенерирован: cmdb={}, process_id={}, docId={}, structurizr_id={}, "
                    + "ответ={}", cmdb, processId, docId, structurizrId, response);
            execution.setVariable(KTS_GENERATED_VARIABLE, true);
            saveAlias(processId, STATUS_DONE, typeProcess);
        } catch (Exception e) {
            String error = "Ошибка генерации паспорта КТС в SmartKTS: " + e.getMessage();
            log.error("{} (cmdb={}, process_id={}, docId={}, structurizr_id={})",
                    error, cmdb, processId, docId, structurizrId, e);
            execution.setVariable(ERROR_VARIABLE, error);
            saveAlias(processId, STATUS_ERROR, typeProcess);
        }
    }

    private String resolveStructurizrId(String cmdb) {
        ResponseEntity<ProductDTO> response = productClient.getProductByCmdb(cmdb);
        if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("не удалось получить продукт " + cmdb + " из Product Service"
                    + (response == null ? "" : ": HTTP " + response.getStatusCode().value()));
        }
        return extractStructurizrId(response.getBody().getStructurizrApiUrl());
    }

    static String extractStructurizrId(String structurizrApiUrl) {
        if (structurizrApiUrl == null || structurizrApiUrl.isBlank()) {
            throw new IllegalStateException("у продукта не задан structurizr_api_url");
        }
        String error = "не удалось выделить structurizr_id из structurizr_api_url: " + structurizrApiUrl;
        String path;
        try {
            path = URI.create(structurizrApiUrl.trim()).getPath();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(error, e);
        }
        List<String> segments = path == null ? List.of()
                : Arrays.stream(path.split("/")).filter(segment -> !segment.isBlank()).toList();
        int share = segments.indexOf("share");
        if (share >= 0) {
            if (share + 1 >= segments.size()) {
                throw new IllegalStateException(error);
            }
            return segments.get(share + 1);
        }
        if (segments.isEmpty()) {
            throw new IllegalStateException(error);
        }
        return segments.get(segments.size() - 1);
    }

    private static String bwikiKtsLink(DelegateExecution execution) {
        Object value = execution.getVariable(BWIKI_KTS_LINK_VARIABLE);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
    }
}
