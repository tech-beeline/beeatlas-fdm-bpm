/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.DocumentClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.StructurizrClient;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.dto.DocIdDTO;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;

@Slf4j
@Component("ExportJsonWorkspaceDelegate")
public class ExportJsonWorkspaceDelegate implements JavaDelegate {

    @Autowired
    ProductClient productClient;
    @Autowired
    StructurizrClient structurizrClient;
    @Autowired
    DocumentClient documentClient;
    @Autowired
    ContextRepository contextRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Выгрузка json workspace");
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        log.info("cmdb: {}, process_id: {}", cmdb, processId);
        ProductDTO productDTO = productClient.getProductInfoByCmdb(cmdb);
        log.info("Получение данных из сервиса продуктов по cmdb, имя продукта: {}", productDTO.getName());
        if (productDTO.getStructurizrApiUrl() == null || productDTO.getStructurizrApiUrl().isEmpty()) {
            throw new ValidationException("не заполнено поле StructurizrApiUrl.");
        }
        String json = structurizrClient.getDocs(productDTO.getStructurizrApiUrl());
        DocIdDTO docIdDTO = documentClient.postDocument(json);
        log.info("Создание документа с ID: {} по cmdb: {}", docIdDTO.getDocId(), cmdb);
        delegateExecution.setVariable("docId", docIdDTO.getDocId());
        log.info("docIdDTO: {}", docIdDTO);
        contextRepository.save(Context.builder()
                .name("docId")
                .value(docIdDTO.getDocId().toString())
                .camundaProcessId(processId)
                .build());
        log.info("завершен Шаг: Выгрузка json workspace");
    }
}
