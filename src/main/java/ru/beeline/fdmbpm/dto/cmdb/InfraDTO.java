package ru.beeline.fdmbpm.dto.cmdb;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfraDTO {

    private String name;
    private String type;
    private String cmdbId;
    private Map<String, String> properties;
}
