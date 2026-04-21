package ru.beeline.fdmbpm.dto.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessStatusItemDTO {
    private Integer id;
    private Integer typeProcessId;
    private String name;
    private String alias;
    private Boolean isDone;
    private Boolean isError;
    private Integer sequence;
}

