package ru.beeline.fdmbpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriteriaExcelDTO {

    private String type;
    private String code;
    private String criterionName;
    private String value;
    private String grade;
    private String comment;
}
