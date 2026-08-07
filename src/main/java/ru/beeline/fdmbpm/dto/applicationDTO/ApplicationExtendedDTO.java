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
public class ApplicationExtendedDTO {

    private Integer id;
    @JsonProperty("entity_id")
    private Integer entityId;
    @JsonProperty("business_key")
    private String businessKey;
    private ApplicationTypeDTO type;
    private ApplicationStatusShortDTO status;
    private AuthorDTO author;
    private AuthorDTO executor;
    private String name;
    @JsonProperty("responsible_id")
    private Integer responsibleId;
    @JsonProperty("create_date")
    private LocalDateTime createDate;
    @JsonProperty("update_date")
    private LocalDateTime updateDate;
    private List<ApplicationCommentDTO> comments;
}
