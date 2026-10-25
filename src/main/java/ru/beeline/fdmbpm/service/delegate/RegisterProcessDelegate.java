/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.beeline.fdmbpm.client.DocumentClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.exception.CmdberrException;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.exception.VldterrException;
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
    DocumentClient documentClient;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    ProductClient productClient;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    ObjectMapper objectMapper;

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
        boolean branchProcess = DatapipeBranchProcess.isBranchProcess(delegateExecution);
        String branch = branchProcess ? DatapipeBranchProcess.branch(delegateExecution) : null;
        log.info("RegisterProcessDelegate: processDefinitionId={}, branch={}",
                delegateExecution.getProcessDefinitionId(), branch);
        TypeProcess typeProcess = typeProcessRepository.findByAlias(branchProcess
                ? DatapipeBranchProcess.TYPE_ALIAS : DatapipeBranchProcess.DATAPIPE_TYPE_ALIAS);
        log.info("RegisterProcessDelegate: typeProcess={}", typeProcess.toString());
        ResponseEntity<ProductDTO> productDTOResponseEntity = productClient.getProductByCmdb(cmdb);
        CamundaProcess camundaProcess = null;
        try {
            camundaProcess = saveProcess(typeProcess.getId(), processId, businessKey);
            if (branchProcess) {
                if (docId == null) {
                    log.info("ℹ️ docId is null, для процесса ветки docId обязателен");
                    throw new VldterrException("vldterr");
                }
                validateBranch(branch);
            }
            if (productDTOResponseEntity.getStatusCode().is2xxSuccessful()) {
                log.info("RegisterProcessDelegate: camundaProcess={}", camundaProcess.getId());
                delegateExecution.setVariable("process_id", camundaProcess.getId());
                log.info("ℹ️ saveAlias: has been saved");
                saveToContextRepository("cmdb", cmdb, camundaProcess);
                if (branchProcess) {
                    saveBranchContext(cmdb, branch, camundaProcess);
                }
                log.info("context: has been saved");
                if (docId != null) {
                    saveToContextRepository("docId", docId.toString(), camundaProcess);
                    ResponseEntity<byte[]> response = documentClient.getDocument(docId);
                    String workspaceCmdb = null;
                    if (isJson(response.getHeaders().getContentDisposition().getFilename())) {
                        workspaceCmdb = extractWorkspaceCmdb(response);
                        if (workspaceCmdb != null && !workspaceCmdb.isEmpty()) {
                            log.info("ℹ️ workspaceСmdb = {}", workspaceCmdb);
                            if (cmdb.equalsIgnoreCase(workspaceCmdb)) {
                                log.info("ℹ️ cmdb equals workspaceСmdb, Saving a process with a status: crt");
                                saveAlias(camundaProcess.getId(), "crt", typeProcess);
                            } else {
                                log.info("ℹ️ cmdb not equals workspaceСmdb, Saving a process with a status: cmdberr");
                                throw new CmdberrException("cmdberr");
                            }
                        } else {
                            log.info("ℹ️ workspaceСmdb == null");
                            throw new VldterrException("vldterr");
                        }
                    } else {
                        log.info("ℹ️ Формат файла не json.");
                        throw new VldterrException("vldterr");
                    }
                } else {
                    log.info("ℹ️ docId is null");
                    saveAlias(camundaProcess.getId(), "crt", typeProcess);
                }
            } else {
                throw new ProcessException(productDTOResponseEntity.getStatusCode().toString());
            }
        } catch (VldterrException e) {
            saveErrStatus("vldterr", processId, businessKey, typeProcess, cmdb, docId, branchProcess, branch);
        } catch (CmdberrException e) {
            saveErrStatus("cmdberr", processId, businessKey, typeProcess, cmdb, docId, branchProcess, branch);
        } catch (Exception e) {
            saveErrStatus("errcrt", processId, businessKey, typeProcess, cmdb, docId, branchProcess, branch);
        }
        log.info("ℹ️Завершение метода: Получение информации и регистрации процесса");
    }

    private void saveErrStatus(String statusAlias, String processId, String businessKey, TypeProcess typeProcess
            , String cmdb, Integer docId, boolean branchProcess, String branch) {
        log.error("❌ Ошибка при регистрации процесса. Создание записи с ошибкой");
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.execute(status -> {
            if (camundaProcessRepository.findByProcIdAndBusinessKey(processId, businessKey).isEmpty()) {
                CamundaProcess failedProcess = saveProcess(typeProcess.getId(), processId, businessKey);
                saveAlias(failedProcess.getId(), statusAlias, typeProcess);
                saveToContextRepository("cmdb", cmdb, failedProcess);
                if (docId != null) {
                    saveToContextRepository("docId", docId.toString(), failedProcess);
                }
                if (branchProcess) {
                    saveBranchContext(cmdb, branch, failedProcess);
                }
            }
            return null;
        });
        throw new ProcessException("❌ Ошибка процесса на шаге: Получение информации и регистрации процесса");
    }

    private static void validateBranch(String branch) {
        if (branch == null || branch.isEmpty()) {
            throw new ProcessException("Не задана ветка branch");
        }
        if (branch.length() > DatapipeBranchProcess.MAX_BRANCH_LENGTH) {
            throw new ProcessException("Длина ветки branch больше " + DatapipeBranchProcess.MAX_BRANCH_LENGTH);
        }
    }

    /**
     * Контекст процесса ветки: branch и artifactUid пайпов staging, которые процесс запустит.
     * Значения, которые ещё неизвестны, не пишутся.
     */
    private void saveBranchContext(String cmdb, String branch, CamundaProcess camundaProcess) {
        if (branch != null && !branch.isEmpty()) {
            saveToContextRepository(DatapipeBranchProcess.BRANCH_VARIABLE, branch, camundaProcess);
        }
        if (cmdb != null) {
            for (StagingArtifact artifact : StagingArtifact.values()) {
                saveToContextRepository(artifact.artifactUidContextName(), cmdb, camundaProcess);
            }
        }
    }

    private boolean isJson(String contentDisposition) {
        if (contentDisposition == null) {
            return false;
        }
        log.info("!!! contentDisposition: {}", contentDisposition);
        return contentDisposition.toLowerCase().contains(".json");
    }

    private String extractWorkspaceCmdb(ResponseEntity<byte[]> response) {
        try {
            byte[] body = response.getBody();
            JsonNode root = objectMapper.readTree(body);
            return root.at("/model/properties/workspace_cmdb").asText();
        } catch (Exception e) {
            log.info("❌ Не найден workspace_cmdb из JSON");
            return null;
        }
    }

    private void saveToContextRepository(String name, String value, CamundaProcess camundaProcess) {
        contextRepository.save(Context.builder()
                .name(name)
                .value(value)
                .camundaProcessId(camundaProcess.getId())
                .build());
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
