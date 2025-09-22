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
@Component("LocalGraphCheckDelegate")
public class LocalGraphCheckDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    GraphClient graphClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Проверка готовности локального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer localGraphCount = (Integer) delegateExecution.getVariable("localGraphCount");
        if (localGraphCount != null) {
            if (localGraphCount > 3) {
                camundaProcessRepository.markAsyncTrueIfFalse(processId);
            }
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            try {
                GraphDTO localGraphDTO = graphClient.getLocalGraph(processId);
                if (localGraphDTO.getStatus().equals("PROCESS")) {
                    delegateExecution.setVariable("localGraphCount", localGraphCount + 1);
                }
                if (localGraphDTO.getStatus().equals("DONE")) {
                    delegateExecution.setVariable("doneLocalGraph", true);
                    saveAlias(processId, "lclgrph", typeProcess);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                saveAlias(processId, "errlclgrph", typeProcess);
                throw new RuntimeException(e.getMessage());
            }
        } else {
            delegateExecution.setVariable("localGraphCount", 1);
        }
    }
}
