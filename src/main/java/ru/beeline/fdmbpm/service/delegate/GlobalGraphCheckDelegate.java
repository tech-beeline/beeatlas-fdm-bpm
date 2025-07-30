package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.graph.GraphDTO;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("GlobalGraphCheckDelegate")
public class GlobalGraphCheckDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    GraphClient graphClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Проверка готовности глобального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer globalGraphCount = (Integer) delegateExecution.getVariable("globalGraphCount");
        if (globalGraphCount != null) {
            if (globalGraphCount > 3) {
                camundaProcessRepository.markAsyncTrueIfFalse(processId);
            }
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            try {
                GraphDTO localGraphDTO = graphClient.getGlobalGraph(processId);
                if (localGraphDTO.getStatus().equals("PROCESS")) {
                    delegateExecution.setVariable("globalGraphCount", globalGraphCount + 1);
                }
                if (localGraphDTO.getStatus().equals("DONE")) {
                    delegateExecution.setVariable("doneLocalGraph ", true);
                    saveAlias(processId, "glblgrph", typeProcess);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                saveAlias(processId, "errglblgrph", typeProcess);
                throw new RuntimeException(e.getMessage());
            }
        } else {
            delegateExecution.setVariable("localGraphCount", 1);
        }
    }
}
