/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.product;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductDTO {
    private Integer id;
    private String name;
    private String alias;
    private String description;
    private String gitUrl;
    private String structurizrWorkspaceName;
    private String structurizrApiKey;
    private String structurizrApiSecret;
    private String structurizrApiUrl;
    private List<TechProductDTO> techProducts;
}
