/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.cmdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDTO {

    private String instanceId;
    private String requestId;
    private String reconciliationId;
    private String internalId;
    private String datasetId;
    private String classId;
    private Long createDate;
    private Long modifiedDate;
    private Boolean markAsDeleted;
    private String name;
    private String shortDescription;
    private String vimDescription;
    private Integer status;
    private String vimStatus;
    private String priority;
    private String item;
    private String vimCMDBGroupCiId;
    private String vimSelResponsobilityIsPerso;
    private String vimSelResponsibilityIsAdvM;
    private AssetAdditionalDataDTO assetAdditionalData;
    private String classTitle;
}

