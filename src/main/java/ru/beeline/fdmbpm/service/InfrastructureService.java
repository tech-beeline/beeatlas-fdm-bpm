package ru.beeline.fdmbpm.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CmdbClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.cmdb.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InfrastructureService {

    @Autowired
    CmdbClient cmdbClient;

    @Autowired
    ProductClient productClient;

    public void gettingApplicationData(String product) {
        CmdbResponseDTO cmdbResponse = cmdbClient.getCmdbInfrastructure(product);
        Map<String, AssetDTO> assetDTOMap = cmdbResponse.getInfrastructureAssets();
        List<InfraDTO> infraDTOList = new ArrayList<>();
        assetDTOMap.forEach((key, value) -> {
            Map<String, String> properties = new HashMap<>();
            switch (value.getClassTitle()) {
                case "База данных" -> properties.put("item", value.getItem());
                case "Серверный домен (виртуальный)" -> {
                    properties.put("item", value.getItem());
                    properties.put("vimServerDomainCpuCores", value.getAssetAdditionalData().getVimServerDomainCpuCores());
                    properties.put("vimServerDomainCpuType", value.getAssetAdditionalData().getVimServerDomainCpuType());
                    properties.put("totalPhysicalMemory", value.getAssetAdditionalData().getTotalPhysicalMemory());
                    properties.put("vimServerDomainLogicalDrives", value.getAssetAdditionalData().getVimServerDomainLogicalDrives());
                    properties.put("vimIp", value.getAssetAdditionalData().getVimIp());
                    properties.put("vimServerDomainOs", value.getAssetAdditionalData().getVimServerDomainOs());
                }
                case "Экземпляр приложения" ->
                        properties.put("appEnvType", value.getAssetAdditionalData().getAppEnvType());
            }
            infraDTOList.add(InfraDTO.builder()
                    .name(value.getName())
                    .type(value.getClassTitle())
                    .cmdbId(value.getInstanceId())
                    .properties(properties)
                    .build());
        });
        Map<String, List<String>> infrastructureAssetsRelation = cmdbResponse.getInfrastructureAssetsRelation();
        List<RelationsDTO> relationsDTOList = new ArrayList<>();
        infrastructureAssetsRelation.forEach((key, value) -> {
            relationsDTOList.add(RelationsDTO.builder()
                    .cmdbId(key)
                    .children(value)
                    .build());
        });
        PostProductRequest postProductRequest = PostProductRequest.builder()
                .infra(infraDTOList)
                .relations(relationsDTOList)
                .build();
        productClient.postProductCMDB(product, postProductRequest);
    }
}
