package ru.beeline.fdmbpm.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmdbResponsibilityDTO {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("instanceId")
    private String instanceId;

    @JsonProperty("assetInstanceId")
    private String assetInstanceId;

    @JsonProperty("personRole")
    private String personRole;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("loginName")
    private String loginName;

    @JsonProperty("vimChrPersonRoleID")
    private String vimChrPersonRoleID;

    @JsonProperty("vimChrPersonRoleTitle")
    private String vimChrPersonRoleTitle;

    @JsonProperty("people")
    private PeopleDTO people;

    @JsonProperty("statusId")
    private String statusId;

    @JsonProperty("organizationTitle")
    private String organizationTitle;

    @JsonProperty("departmentTitle")
    private String departmentTitle;

    @JsonProperty("vimCMDBGroupCiID")
    private String vimCMDBGroupCiID;

    @JsonProperty("vimSelRespRoleType")
    private String vimSelRespRoleType;

}
