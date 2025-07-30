package ru.beeline.fdmbpm.dto.mapic;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MethodDTO {
    private String name;
    private String context;
    private String description;
    private String type;
    private String returnType;
    private List<ParameterDTO> parameters;
}
