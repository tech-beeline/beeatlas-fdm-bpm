/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.beeline.fdmbpm.dto.LinkDTO;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DashboardProductsDTO {

    private String name;
    private String code;
    private String version;
    private String author;
    @JsonProperty("FQName")
    private String fqName;
    private String status;
    private Date modifiedDate;
    private LinkDTO links;
}
