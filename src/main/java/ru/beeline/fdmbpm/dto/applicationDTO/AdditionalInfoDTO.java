package ru.beeline.fdmbpm.dto.applicationDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalInfoDTO {

    private String domainName;
    private Integer orderBcId;
}
