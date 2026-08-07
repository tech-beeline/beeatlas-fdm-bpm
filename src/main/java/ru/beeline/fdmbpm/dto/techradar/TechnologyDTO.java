/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.techradar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnologyDTO {

    private Integer id;
    private Boolean isCritical;
    private String label;
    private RingDTO ring;
    private SectorDTO sector;
}

