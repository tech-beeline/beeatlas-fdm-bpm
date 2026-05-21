/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.techradar;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PatternDTO {

    private String code;
    private String createDate;
    private String deleteDate;
    private String description;
    private String dsl;
    private List<GroupDTO> groups;
    private Integer id;
    private Boolean isAntiPattern;
    private String name;
    private String rule;
    private List<TechnologyDTO> technologies;
    private String updateDate;
}

