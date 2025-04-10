package ru.beeline.fdmbpm.dto.cmdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmdbResponseDTO {

    private String status;
    private String statusMessage;
    private AssetDTO asset;
    private Map<String, AssetDTO> infrastructureAssets;
    private Map<String, List<String>> infrastructureAssetsRelation;
}
