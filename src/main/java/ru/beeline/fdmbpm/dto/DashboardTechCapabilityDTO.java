package ru.beeline.fdmbpm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DashboardTechCapabilityDTO {

    private String code;
    private String name;
    private String description;
    private String author;
    private Date modifiedDate;
    private String status;
    private String owner;
    private List<String> parents;
    private String targetSystemCode;
    private List<String> relatedSystems;
}
