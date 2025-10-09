package ru.beeline.fdmbpm.dto.cmdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeopleDTO {

    private String requestId;
    private String instanceId;
    private String profileStatus;
    private String reconciliationId;
    private String remedyLoginId;
    private String loginMsad;
    private String fullName;
    private String lastName;
    private String firstName;
    private String middleInitial;
    private String jobTitle;
    private String organization;
    private String department;
    private String phoneNumberBusiness;
    private String phoneNumberMobile;
    private String internetEmail;
    private String corporateId;
    private String managersName;
    private String vimChrAuthLogin;
}
