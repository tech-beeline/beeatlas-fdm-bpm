package ru.beeline.fdmbpm.dto.camundaProcess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusDTO {

    private Integer id;
    private String name;
    private String alias;
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
}

