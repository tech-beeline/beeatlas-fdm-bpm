/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.camundaProcess;


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
    private String procId;
    private String businessKey;
    private TypeDTO type;
    private StatusDTO status;
}