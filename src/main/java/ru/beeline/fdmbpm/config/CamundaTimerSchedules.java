/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("camundaTimerSchedules")
@Getter
public class CamundaTimerSchedules {

    private final String cmdbFullCycle;
    private final String capabilityUploadCycle;
    private final String dailyS3CleanupCycle;
    private final String relationsFromGitCycle;
    private final String uploadProductForMapicCycle;
    private final String userProductAllCycle;

    public CamundaTimerSchedules(
            @Value("${fdm.camunda.timer.cmdb-full-cycle:R/P1Y}") String cmdbFullCycle,
            @Value("${fdm.camunda.timer.capability-upload-cycle:R/2024-04-15T00:00:00+03:00/P1D}") String capabilityUploadCycle,
            @Value("${fdm.camunda.timer.daily-s3-cleanup-cycle:R/P1D}") String dailyS3CleanupCycle,
            @Value("${fdm.camunda.timer.relations-from-git-cycle:R/2024-01-08T00:00:00+03:00/P1M}") String relationsFromGitCycle,
            @Value("${fdm.camunda.timer.upload-product-for-mapic-cycle:R/2025-08-15T00:00:00+03:00/P1D}") String uploadProductForMapicCycle,
            @Value("${fdm.camunda.timer.user-product-all-cycle:R/P1D}") String userProductAllCycle) {
        this.cmdbFullCycle = cmdbFullCycle;
        this.capabilityUploadCycle = capabilityUploadCycle;
        this.dailyS3CleanupCycle = dailyS3CleanupCycle;
        this.relationsFromGitCycle = relationsFromGitCycle;
        this.uploadProductForMapicCycle = uploadProductForMapicCycle;
        this.userProductAllCycle = userProductAllCycle;
    }
}
