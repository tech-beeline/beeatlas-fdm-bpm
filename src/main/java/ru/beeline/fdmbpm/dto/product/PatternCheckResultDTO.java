/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.product;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PatternCheckResultDTO {

    private String code;
    private Boolean isCheck;
    private String resultDetails;
}

