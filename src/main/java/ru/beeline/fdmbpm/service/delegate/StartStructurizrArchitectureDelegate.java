/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import org.springframework.stereotype.Component;

@Component("StartStructurizrArchitectureDelegate")
public class StartStructurizrArchitectureDelegate extends StartStagingPipelineDelegate {

    public StartStructurizrArchitectureDelegate() {
        super(StagingArtifact.ARCHITECTURE);
    }
}
