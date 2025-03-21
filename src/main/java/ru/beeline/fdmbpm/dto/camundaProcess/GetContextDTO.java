package ru.beeline.fdmbpm.dto.camundaProcess;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetContextDTO {

    private Integer id;
    @JsonProperty("proc_id")
    private String procId;
    @JsonProperty("business_key")
    private String businessKey;
    private TypeDTO type;
    private StatusDTO status;
}