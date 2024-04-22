package ru.beeline.fdmbpm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCapabilityDTO {

    private String code;
    private boolean isDomain;
    private String name;
    private String description;
    private String author;
    private Date modifiedDate;
    private String status;
    private String owner;
}
