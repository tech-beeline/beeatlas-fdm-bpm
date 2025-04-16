package ru.beeline.fdmbpm.dto.camundaProcess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmlib.dto.auth.RoleTypeDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleInfoDTO {

    private Long id;
    private String name;
    private String descr;
    private RoleTypeDTO alias;
    private boolean deleted;
}
