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
public class AssetAdditionalDataDTO {

    private String instanceId;
    private String type;
    private String serialNumber;
    private String vimServerDomainCpuCores;
    private String vimServerDomainCpuType;
    private String frequency;
    private String totalPhysicalMemory;
    private String vimServerDomainLogicalDrives;
    private String vimIp;
    private String vimServerDomainOs;
    private String appEnvType;
}
