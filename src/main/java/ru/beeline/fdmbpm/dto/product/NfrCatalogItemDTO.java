/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class NfrCatalogItemDTO {

    private Integer id;
    private String code;
    private Integer version;
    private String name;
    private String description;
    private String rule;
    private String source;
}
