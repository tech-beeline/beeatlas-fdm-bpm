/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.cmdb;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PostProductRequest {

    private List<InfraDTO> infra;
    private List<RelationsDTO> relations;
}
