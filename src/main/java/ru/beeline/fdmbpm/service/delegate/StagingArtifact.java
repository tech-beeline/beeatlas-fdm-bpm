/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.Getter;

/**
 * Пайпы staging, которые запускает Process_datapipeline_branch, и статусы data-pipe для каждого из них.
 */
@Getter
public enum StagingArtifact {

    ARCHITECTURE("structurizr-architecture", "выгрузка архитектуры, технических возможностей и технологий",
            "stgarchcrt", "stgarchdn", "stgarchdn", "stgarcherr"),
    SEQUENCE("structurizr-sequence", "выгрузка dynamic view",
            "stgseqcrt", "stgseqdn", "stgseqwop", "stgseqerr");

    private final String type;
    private final String title;
    private final String startedAlias;
    private final String completedAlias;
    private final String completedWithoutPublishAlias;
    private final String errorAlias;

    StagingArtifact(String type, String title, String startedAlias, String completedAlias,
                    String completedWithoutPublishAlias, String errorAlias) {
        this.type = type;
        this.title = title;
        this.startedAlias = startedAlias;
        this.completedAlias = completedAlias;
        this.completedWithoutPublishAlias = completedWithoutPublishAlias;
        this.errorAlias = errorAlias;
    }

    public String artifactUidContextName() {
        return "artifactUid." + type;
    }

    public String runIdContextName() {
        return "runId." + type;
    }

    /**
     * Статус data-pipe для терминального статуса прогона staging, null — прогон ещё не завершён.
     */
    public String terminalAlias(String stagingStatus) {
        if (stagingStatus == null) {
            return null;
        }
        return switch (stagingStatus) {
            case "completed" -> completedAlias;
            case "completed_without_publish" -> completedWithoutPublishAlias;
            case "failed", "cancelled" -> errorAlias;
            default -> null;
        };
    }

    public static StagingArtifact byType(String type) {
        for (StagingArtifact artifact : values()) {
            if (artifact.type.equals(type)) {
                return artifact;
            }
        }
        throw new IllegalArgumentException("Неизвестный тип пайпа staging: " + type);
    }
}
