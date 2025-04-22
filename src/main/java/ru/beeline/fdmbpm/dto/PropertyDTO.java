package ru.beeline.fdmbpm.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PropertyDTO {
    private String key;
    private String value;
}
