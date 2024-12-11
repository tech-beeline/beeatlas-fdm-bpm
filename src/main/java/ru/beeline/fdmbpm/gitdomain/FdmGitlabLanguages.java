package ru.beeline.fdmbpm.gitdomain;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;

@ToString
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_fdm_gitlab_languages", schema = "public")
public class FdmGitlabLanguages {
    @Id
    private String cmdb_code;
    private String proj_lang;
    private Date extraction_date;
}
