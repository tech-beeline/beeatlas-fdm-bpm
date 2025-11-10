package ru.beeline.fdmbpm.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CmdbClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.PropertyDTO;
import ru.beeline.fdmbpm.dto.cmdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class InfrastructureService {

    @Autowired
    CmdbClient cmdbClient;

    @Autowired
    ProductClient productClient;

    @Autowired
    RabbitService rabbitService;

    @Autowired
    ObjectMapper objectMapper;

    public void gettingApplicationData(String product) {
        CmdbResponseDTO cmdbResponse = cmdbClient.getCmdbInfrastructure(product);
        if (cmdbResponse == null) {
            log.info("Запрос к CMDB /api/cmdb/reports/infrastructure_on_asset_report/ c product вернул:  " + product + " = null");
            return;
        }
        log.info("✅ Успешно получен отчет об инфраструктуре CMDB для продукта '{}'.", product);
        List<CmdbResponsibilityDTO> responsibilities = cmdbClient.getCmdbResponsibilities(cmdbResponse.getAsset()
                .getReconciliationId());
        if (!responsibilities.isEmpty()) {
            log.info("responsibilities size: {}", responsibilities.size());
            sendMessage(product, cmdbResponse, responsibilities);
        } else {
            log.info("⚠️ responsibilities is empty");
        }
        Map<String, AssetDTO> assetDTOMap = cmdbResponse.getInfrastructureAssets();
        List<InfraDTO> infraDTOList = new ArrayList<>();
        assetDTOMap.forEach((key, value) -> {
            List<PropertyDTO> properties = new ArrayList<>();
            switch (value.getClassTitle()) {
                case "База данных" -> properties.add(new PropertyDTO("item", value.getItem()));
                case "Серверный домен (виртуальный)" -> {
                    properties.add(new PropertyDTO("item", value.getItem()));
                    properties.add(new PropertyDTO("vimServerDomainCpuCores",
                            value.getAssetAdditionalData().getVimServerDomainCpuCores()));
                    properties.add(new PropertyDTO("vimServerDomainCpuType",
                            value.getAssetAdditionalData().getVimServerDomainCpuType()));
                    properties.add(new PropertyDTO("totalPhysicalMemory",
                            value.getAssetAdditionalData().getTotalPhysicalMemory()));
                    properties.add(new PropertyDTO("vimServerDomainLogicalDrives",
                            value.getAssetAdditionalData().getVimServerDomainLogicalDrives()));
                    properties.add(new PropertyDTO("vimIp", value.getAssetAdditionalData().getVimIp()));
                    properties.add(new PropertyDTO("vimServerDomainOs",
                            value.getAssetAdditionalData().getVimServerDomainOs()));
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
            relationsDTOList.add(RelationsDTO.builder().cmdbId(key).children(value).build());
        });
        PostProductRequest postProductRequest = PostProductRequest.builder()
                .infra(infraDTOList)
                .relations(relationsDTOList)
                .build();
        productClient.postProductCMDB(product, postProductRequest);
        log.info("Успешная синхронизация инфраструктуры продукта");
    }

    private void sendMessage(String product, CmdbResponseDTO cmdbResponse,
                             List<CmdbResponsibilityDTO> responsibilities) {
        try {
            Optional<CmdbResponsibilityDTO> ownerResponsibility = responsibilities.stream()
                    .filter(responsibility -> responsibility.getVimChrPersonRoleTitle().equals("Владелец приложения"))
                    .findFirst();
            if (ownerResponsibility.isEmpty()) {
                log.warn("⚠️ Не найден владелец приложения для продукта: {}. Сообщение не отправлено.", product);
                return;
            }
            PeopleDTO peopleDto = ownerResponsibility.get().getPeople();
            ObjectNode item = objectMapper.createObjectNode();
            item.put("cmdb", product);
            item.put("critical", cmdbResponse.getAsset().getPriority());

            ObjectNode ownerNode = objectMapper.createObjectNode();
            ownerNode.put("fullName", peopleDto.getFullName());
            ownerNode.put("email", peopleDto.getInternetEmail());
            ownerNode.put("extId", peopleDto.getCorporateId());
            ownerNode.put("login", peopleDto.getLoginMsad());
            item.set("owner", ownerNode);
            log.info("Send to update-product-owner-and-priority-by-cmdb");
            rabbitService.sendMessage("update-product-owner-and-priority-by-cmdb", objectMapper.writeValueAsString(item));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
