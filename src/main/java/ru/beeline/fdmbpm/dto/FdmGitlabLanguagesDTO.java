package ru.beeline.fdmbpm.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class FdmGitlabLanguagesDTO {
    private String cmdb_code;
    private String proj_lang;
}
