/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApplicationProcessDTO {

    private String processInstanceId;
    private String businessKey;
    private Integer authorId;
    private String type;
    private String comment;
    private Integer entityId;
    private String name;
}
