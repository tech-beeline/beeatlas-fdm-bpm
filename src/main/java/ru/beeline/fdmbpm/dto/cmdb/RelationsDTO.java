/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.cmdb;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RelationsDTO {

    private String cmdbId;
    private List<String> children;
}
