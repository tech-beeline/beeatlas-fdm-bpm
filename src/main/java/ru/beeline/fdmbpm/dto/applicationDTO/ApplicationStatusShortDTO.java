package ru.beeline.fdmbpm.dto.applicationDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusShortDTO {

    private Integer id;
    private String name;
    private Boolean isEndStatus;
}
