/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.bw;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VcsRepositoryDTO {

    private Integer id;
    private String name;
    private String description;
    private String cmdbCode;
    private VcsRepositoryDTO vcsRepository;
}
