package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.DocumentServiceClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.StructurizrClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.DocIdDTO;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.ContextRepository;
import ru.beeline.fdmbpm.repository.TypeProcessRepository;
import ru.beeline.fdmbpm.service.ExportProcessService;

@Slf4j
@Component("ExportJsonWorkspaceDelegate")
public class ExportJsonWorkspaceDelegate implements JavaDelegate {

    @Autowired
    ProductClient productClient;
    @Autowired
    StructurizrClient structurizrClient;
    @Autowired
    DocumentServiceClient documentServiceClient;
    @Autowired
    ContextRepository contextRepository;

    @Override
    public void execute(DelegateExecution delegateExecution)  {
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        ProductDTO productDTO = productClient.getProductInfoByCmdb(cmdb);
        String json = structurizrClient.getDocs(productDTO.getStructurizrApiUrl());
        DocIdDTO docIdDTO = documentServiceClient.postDocument(json);
        contextRepository.save(Context.builder()
                .name("docId")
                .value(docIdDTO.getDocId().toString())
                .camundaProcessId(processId)
                .build());
    }
}
