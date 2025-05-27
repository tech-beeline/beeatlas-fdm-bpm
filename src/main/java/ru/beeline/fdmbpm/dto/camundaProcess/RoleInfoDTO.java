package ru.beeline.fdmbpm.dto.camundaProcess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleInfoDTO {

    private Long id;
    private String name;
    private String descr;
    private String alias;
    private boolean deleted;
}
