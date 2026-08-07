/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.dto.product.DiscoveredInterfaceDTO;
import ru.beeline.fdmbpm.dto.product.PublishedApiDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("MapicInterfaceFetcherDelegate")
public class MapicInterfaceFetcherDelegate implements JavaDelegate {

    @Autowired
    ProductClient productClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("MapicInterfaceFetcherDelegate start");
        for (Map.Entry<String, Object> entry : delegateExecution.getVariables().entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        String product = (String) delegateExecution.getVariable("product");
        log.info("product is" + product);
        if (product == null) {
            throw new RuntimeException("product is null");
        }
        log.info("MapicInterfaceFetcherDelegate get product");
        ProductDTO productDto = productClient.getProduct(product);
        if (productDto == null) {
            throw new RuntimeException("Запрос в сервис продуктов, productDto is null");
        }
        log.info("MapicInterfaceFetcherDelegate get interfaces");
        List<PublishedApiDTO> publishedApiDTOS = productClient.getInterfaces(product);
        log.info("MapicInterfaceFetcherDelegate iterator");
        List<Integer> apiIds = new ArrayList<>();
        if (!publishedApiDTOS.isEmpty()) {
            publishedApiDTOS.forEach(publishedApiDTO -> {
                if (publishedApiDTO.getStatusName().equals("Deployed") || publishedApiDTO.getStatusName()
                        .equals("Hidden")) {
                    productClient.updateInterface(DiscoveredInterfaceDTO.builder()
                            .name(publishedApiDTO.getApiContext())
                            .externalId(publishedApiDTO.getId())
                            .apiId(publishedApiDTO.getApiId())
                            .status(publishedApiDTO.getStatusName())
                            .context(publishedApiDTO.getApiContext())
                            .productId(productDto.getId())
                            .build());
                    apiIds.add(publishedApiDTO.getId());
                }
            });
            log.info("apiIds is " + apiIds);
            delegateExecution.setVariable("apiIds", apiIds);
        }
        log.info("apiIds is empty");
        delegateExecution.setVariable("apiIds", apiIds);
    }
}
