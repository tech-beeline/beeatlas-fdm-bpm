/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import org.springframework.stereotype.Component;

/**
 * Прогон dynamic view останавливается в awaiting_review до apply или decline в staging.
 * Data-pipe только создаёт прогон, решение принимает пользователь.
 */
@Component("StartStructurizrSequenceDelegate")
public class StartStructurizrSequenceDelegate extends StartStagingPipelineDelegate {

    public StartStructurizrSequenceDelegate() {
        super(StagingArtifact.SEQUENCE);
    }
}
