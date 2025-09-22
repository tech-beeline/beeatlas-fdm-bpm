package ru.beeline.fdmbpm.dto.mapic;


import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class MethodDTO {
    private String name;
    private String context;
    private String description;
    private String type;
    private String returnType;
    private List<ParameterDTO> parameters;
}
