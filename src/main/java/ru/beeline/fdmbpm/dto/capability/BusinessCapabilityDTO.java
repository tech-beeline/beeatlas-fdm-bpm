/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.capability;


import lombok.*;

import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessCapabilityDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private String author;
    private String link;
    private String owner;
    private String parents;
    private boolean isDomain;
    private Date createdDate;
    private boolean hasChildren;

    public boolean getIsDomain() {
        return isDomain;
    }
}
