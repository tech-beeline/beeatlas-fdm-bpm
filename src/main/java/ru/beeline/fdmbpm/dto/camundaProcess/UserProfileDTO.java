package ru.beeline.fdmbpm.dto.camundaProcess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {

    private Integer id;
    @JsonProperty("id_ext")
    private String idExt;
    @JsonProperty("full_name")
    private String fullName;
    private String login;
    @JsonProperty("last_login")
    private Date lastLogin;
    private String email;
    private List<RoleInfoDTO> roles;
}
