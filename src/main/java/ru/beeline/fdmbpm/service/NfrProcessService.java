/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NfrProcessService {

    private static final String AUTO_NFR_ALL_PROCESS_KEY = "Process_1t2rjem";

    @Autowired
    private RuntimeService runtimeService;

    public void startAutoAssignProcess(Integer nfrId, List<String> rule) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nfrId", nfrId);
        variables.put("rule", rule != null ? rule : List.of());
        log.info("Запуск процесса auto-nfr-all: nfrId={}, rule={}", nfrId, rule);
        runtimeService.startProcessInstanceByKey(AUTO_NFR_ALL_PROCESS_KEY, variables);
        log.info("Процесс auto-nfr-all запущен для nfrId={}", nfrId);
    }
}
