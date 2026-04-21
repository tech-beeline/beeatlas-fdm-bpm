package ru.beeline.fdmbpm.dto.techradar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RingDTO {

    private Integer id;
    private String name;
    private Integer order;
}

