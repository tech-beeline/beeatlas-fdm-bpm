package ru.beeline.fdmbpm.dto.applicationDTO;

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
    private String business_key;
    private ApplicationTypeDTO type;
    private ApplicationStatusDTO status;
    private Integer authorId;
    private Integer executorId;
    private String name;
    private Integer responsibleId;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private List<ApplicationCommentDTO> comments;
}
