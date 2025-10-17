package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("RegisterProcessDelegate")
public class RegisterProcessDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    ContextRepository contextRepository;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    ProductClient productClient;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Старт метода: Получение информации и регистрации процесса");
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        log.info("RegisterProcessDelegate: cmdb={}", cmdb);
        String processId = delegateExecution.getProcessInstanceId();
        log.info("RegisterProcessDelegate: processId={}", processId);
        String businessKey = delegateExecution.getBusinessKey();
        log.info("RegisterProcessDelegate: businessKey={}", businessKey);
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        log.info("RegisterProcessDelegate: docId={}", docId);
        TypeProcess typeProcess = typeProcessRepository.findByAlias("Datapipe");
        log.info("RegisterProcessDelegate: typeProcess={}", typeProcess.toString());
        ResponseEntity<ProductDTO> productDTOResponseEntity = productClient.getProductByCmdb(cmdb);
        try {
            CamundaProcess camundaProcess;
            camundaProcess = saveProcess(typeProcess.getId(), processId, businessKey);
            if (productDTOResponseEntity.getStatusCode().is2xxSuccessful()) {
                log.info("RegisterProcessDelegate: camundaProcess={}", camundaProcess.getId());
                delegateExecution.setVariable("process_id", camundaProcess.getId());
                saveAlias(camundaProcess.getId(), "crt", typeProcess);
                log.info("saveAlias: has been saved");
                contextRepository.save(Context.builder()
                        .name("cmdb")
                        .value(cmdb)
                        .camundaProcessId(camundaProcess.getId())
                        .build());
                log.info("context: has been saved");
                if (docId != null) {
                    contextRepository.save(Context.builder()
                            .name("doc_id")
                            .value(docId.toString())
                            .camundaProcessId(camundaProcess.getId())
                            .build());
                }
            } else {
                saveAlias(camundaProcess.getId(), "errcrt", typeProcess);
            }
        } catch (Exception e) {
            log.error("Ошибка при регистрации процесса. Создание записи с ошибкой", e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tt.execute(status -> {
                CamundaProcess failedProcess = saveProcess(typeProcess.getId(), processId, businessKey);
                saveAlias(failedProcess.getId(), "errcrt", typeProcess);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Получение информации и регистрации процесса");
        }
        log.info("Завершение метода: Получение информации и регистрации процесса");
    }

    private CamundaProcess saveProcess(Integer typeProcessId, String processId, String businessKey) {
        return camundaProcessRepository.save(CamundaProcess.builder()
                .typeProcessId(typeProcessId)
                .procId(processId)
                .businessKey(businessKey)
                .isAsync(false)
                .build());
    }
}
