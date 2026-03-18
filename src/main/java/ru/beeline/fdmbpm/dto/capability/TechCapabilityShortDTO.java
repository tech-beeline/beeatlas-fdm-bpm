/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.capability;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import ru.beeline.fdmbpm.dto.product.GetProductsByIdsDTO;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechCapabilityShortDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String type;
    private String author;
    private String owner;
    private String link;
    private Date createdDate;
    @JsonProperty("updatedDate")
    private Date lastModifiedDate;
    private Date deletedDate;
    @JsonIgnore
    private Integer systemId;
    private GetProductsByIdsDTO system;
    private List<CriteriaDTO> criteria;
}
