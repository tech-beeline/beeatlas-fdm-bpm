/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.staging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineRunRequestDTO {

    private String artifactType;
    private String artifactUid;
    private String source;
    private String branch;
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private Integer docId;
    }
}
