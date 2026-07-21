/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmbpm.dto.nfr.NfrAutoAssignRequestDTO;
import ru.beeline.fdmbpm.service.NfrProcessService;

@Slf4j
@RestController
@RequestMapping("/api/v1/nfr")
public class NfrProcessController {

    @Autowired
    private NfrProcessService nfrProcessService;

    @PostMapping("/auto-assign")
    public ResponseEntity<Void> startAutoAssign(@RequestBody NfrAutoAssignRequestDTO request) {
        log.info("POST /api/v1/nfr/auto-assign: nfrId={}", request.getNfrId());
        nfrProcessService.startAutoAssignProcess(request.getNfrId(), request.getRule());
        return ResponseEntity.ok().build();
    }
}
