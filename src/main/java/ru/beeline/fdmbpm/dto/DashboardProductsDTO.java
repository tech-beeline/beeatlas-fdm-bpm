package ru.beeline.fdmbpm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
