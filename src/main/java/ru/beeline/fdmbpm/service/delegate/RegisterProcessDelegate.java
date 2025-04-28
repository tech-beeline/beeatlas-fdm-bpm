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
        String processId = delegateExecution.getProcessInstanceId();
        String businessKey = delegateExecution.getBusinessKey();
        String docId = (String) delegateExecution.getVariable("docId");
        TypeProcess typeProcess = typeProcessRepository.findByAlias("Datapipe");
        CamundaProcess camundaProcess = camundaProcessRepository.save(CamundaProcess.builder()
                .typeProcessId(typeProcess.getId())
                .procId(processId)
                .businessKey(businessKey)
                .build());
        delegateExecution.setVariable("process_id", camundaProcess.getId());

        saveAlias(camundaProcess.getId(), "Datapipe", typeProcess);
        contextRepository.save(Context.builder()
                .name("cmdb")
                .value(cmdb)
                .camundaProcessId(camundaProcess.getId())
                .build());
        if (docId != null) {
            contextRepository.save(Context.builder()
                    .name("doc_id")
                    .value(docId)
                    .camundaProcessId(camundaProcess.getId())
                    .build());
        }
    }
}
