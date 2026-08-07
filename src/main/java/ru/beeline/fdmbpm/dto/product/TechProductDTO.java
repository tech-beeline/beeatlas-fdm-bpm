/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.product;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TechProductDTO {
    private Integer id;
    private Integer techId;
    private String source;
    private LocalDateTime createdDate;
    private LocalDateTime deletedDate;
    private LocalDateTime lastModifiedDate;
}
