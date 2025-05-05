package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.ContextRepository;
import ru.beeline.fdmbpm.repository.TypeProcessRepository;

@Slf4j
@Component("RegisterProcessDelegate")
public class RegisterProcessDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    ContextRepository contextRepository;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;


    @Override
    public void execute(DelegateExecution delegateExecution) {
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        log.info("RegisterProcessDelegate: cmdb=", cmdb);
        String processId = delegateExecution.getProcessInstanceId();
        log.info("RegisterProcessDelegate: processId=", processId);
        String businessKey = delegateExecution.getBusinessKey();
        log.info("RegisterProcessDelegate: businessKey=", businessKey);
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        log.info("RegisterProcessDelegate: docId=", docId);
        TypeProcess typeProcess = typeProcessRepository.findByAlias("Datapipe");
        log.info("RegisterProcessDelegate: typeProcess=", typeProcess.toString());
        CamundaProcess camundaProcess = camundaProcessRepository.save(CamundaProcess.builder()
                .typeProcessId(typeProcess.getId())
                .procId(processId)
                .businessKey(businessKey)
                .build());
        log.info("RegisterProcessDelegate: camundaProcess=", camundaProcess.getId());
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
    }
}
