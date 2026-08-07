/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TokenDTO {
    String token;
    String validTo;
}