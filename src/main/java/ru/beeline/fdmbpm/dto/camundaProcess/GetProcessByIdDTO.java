/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.camundaProcess;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetProcessByIdDTO {

    private Integer id;
    private String procId;
    private String businessKey;
    private TypeDTO type;
    private List<StatusDTO> statuses;
    private List<ShortContextDTO> context;
}
