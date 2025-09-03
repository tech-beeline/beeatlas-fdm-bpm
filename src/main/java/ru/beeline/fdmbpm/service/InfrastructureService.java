package ru.beeline.fdmbpm.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CmdbClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.PropertyDTO;
import ru.beeline.fdmbpm.dto.cmdb.*;
import ru.beeline.fdmbpm.exception.ValidationException;

import java.util.ArrayList;
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
        if (cmdbResponse == null) {
            log.info("Запрос к CMDB /api/cmdb/reports/infrastructure_on_asset_report/ c product вернул:  " +
                    product + " = null");
            return;
        }
        log.info("Успешно получен отчет об инфраструктуре CMDB для продукта '{}'.", product);
        Map<String, AssetDTO> assetDTOMap = cmdbResponse.getInfrastructureAssets();
        List<InfraDTO> infraDTOList = new ArrayList<>();
        assetDTOMap.forEach((key, value) -> {
            List<PropertyDTO> properties = new ArrayList<>();
            switch (value.getClassTitle()) {
                case "База данных" -> properties.add(new PropertyDTO("item", value.getItem()));
                case "Серверный домен (виртуальный)" -> {
                    properties.add(new PropertyDTO("item", value.getItem()));
                    properties.add(new PropertyDTO("vimServerDomainCpuCores", value.getAssetAdditionalData().getVimServerDomainCpuCores()));
                    properties.add(new PropertyDTO("vimServerDomainCpuType", value.getAssetAdditionalData().getVimServerDomainCpuType()));
                    properties.add(new PropertyDTO("totalPhysicalMemory", value.getAssetAdditionalData().getTotalPhysicalMemory()));
                    properties.add(new PropertyDTO("vimServerDomainLogicalDrives", value.getAssetAdditionalData().getVimServerDomainLogicalDrives()));
                    properties.add(new PropertyDTO("vimIp", value.getAssetAdditionalData().getVimIp()));
                    properties.add(new PropertyDTO("vimServerDomainOs", value.getAssetAdditionalData().getVimServerDomainOs()));
                }
                case "Экземпляр приложения" ->
                        properties.add(new PropertyDTO("appEnvType", value.getAssetAdditionalData().getAppEnvType()));
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
        log.info("Успешная синхронизация инфраструктуры продукта");
    }
}
