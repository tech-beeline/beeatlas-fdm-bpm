package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmlib.dto.product.DiscoveredInterfaceDTO;
import ru.beeline.fdmlib.dto.product.PublishedApiDTO;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("MapicInterfaceFetcherDelegate")
public class MapicInterfaceFetcherDelegate implements JavaDelegate {

    @Autowired
    ProductClient productClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("MapicInterfaceFetcherDelegate start");
        String product = (String) delegateExecution.getVariable("product");
        if (product == null) {
            throw new RuntimeException("product is null");
        }
        log.info("MapicInterfaceFetcherDelegate get product");
        ProductDTO productDto = productClient.getProduct(product);
        log.info("MapicInterfaceFetcherDelegate get interfaces");
        List<PublishedApiDTO> publishedApiDTOS = productClient.getInterfaces(product);
        log.info("MapicInterfaceFetcherDelegate iterator");
        if (!publishedApiDTOS.isEmpty()) {
            List<Integer> apiIds = new ArrayList<>();
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
                    apiIds.add(publishedApiDTO.getApiId());
                }
            });
            delegateExecution.setVariable("apiIds", apiIds,toString());
        }
    }
}
