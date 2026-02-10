/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.capability;

import lombok.*;
import ru.beeline.fdmlib.dto.capability.ParentOrMutableDTO;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessCapabilityOrderDraftResponseDTO {

    private Integer id;
    private String name;
    private String code;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime updateDate;
    private String owner;
    private ParentOrMutableDTO parent;
    private String author;
    private ParentOrMutableDTO mutable;
}
