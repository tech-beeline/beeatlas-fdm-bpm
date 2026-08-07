/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.techradar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductDTO {

    @JsonProperty("product_id")
    private Integer productId;
    private String alias;
    private List<ProductTechDTO> tech;
}
