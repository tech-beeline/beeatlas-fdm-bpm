package ru.beeline.fdmbpm.dto.mapic;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ParameterDTO {
    private String parameterName;
    private String parameterType;
}
