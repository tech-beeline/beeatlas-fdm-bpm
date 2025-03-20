package ru.beeline.fdmbpm.dto.camundaProcess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessDTO {

    private Integer id;
    private String name;
    private Integer processId;
    private Boolean isDone;
    private Boolean isError;
    private LocalDateTime createdDate;
}
