package ru.beeline.fdmbpm.dto.graph;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphDTO {

    private Integer id;
    private String taskKey;
    private String status;
    private String type;
}
