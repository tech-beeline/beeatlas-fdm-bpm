package ru.beeline.fdmbpm.dto.cmdb;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class InfrastructureAssetsRelationDTO {

    private Map<String, List<String>> infrastructureAssetsRelation;
}
