package ru.beeline.fdmbpm.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatternCheckResultDTO {

    private String code;
    private Boolean isCheck;
    private String resultDetails;
}

