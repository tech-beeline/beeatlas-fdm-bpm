/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmbpm.domain.Application;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.repository.camunda.ApplicationRepository;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/internal/check")
public class InternalCheckController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @GetMapping("/application/{businessKey}/author-or-executor")
    public ResponseEntity<Map<String, Boolean>> checkAuthorOrExecutor(
            @PathVariable String businessKey,
            @RequestParam Integer userId) {
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Запись с данным businessKey: %s не найдена", businessKey)));
        boolean hasAccess = Objects.equals(application.getAuthorId(), userId)
                || Objects.equals(application.getExecutorId(), userId);
        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }

    @GetMapping("/application/{businessKey}/current-executor")
    public ResponseEntity<Map<String, Boolean>> checkCurrentExecutor(
            @PathVariable String businessKey,
            @RequestParam Integer userId) {
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Запись с данным businessKey: %s не найдена", businessKey)));
        boolean hasAccess = Objects.equals(application.getExecutorId(), userId);
        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }
}
