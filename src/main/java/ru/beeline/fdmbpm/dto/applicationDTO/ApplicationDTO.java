/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.applicationDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDTO {

    private Integer id;
    private Integer entityId;
    private String businessKey;
    private ApplicationTypeDTO type;
    private ApplicationStatusDTO status;
    private ApplicationParticipantDTO author;
    private ApplicationParticipantDTO executor;
    private List<ApplicationAdditionalInfoDTO> additionalInfo;
    private String name;
    private Integer responsibleId;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private List<ApplicationCommentDTO> comments;
}
