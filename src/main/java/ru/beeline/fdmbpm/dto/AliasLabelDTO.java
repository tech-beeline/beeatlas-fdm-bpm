/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AliasLabelDTO {

    private Integer productId;
    private String alias;
    private Integer techId;
    private String label;

}
