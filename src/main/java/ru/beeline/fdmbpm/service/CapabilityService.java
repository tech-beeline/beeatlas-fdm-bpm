package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.BWEmployeeClient;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.client.DashboardClient;
import ru.beeline.fdmbpm.client.PackageClient;
import ru.beeline.fdmbpm.dto.DashboardCapabilityDTO;
import ru.beeline.fdmbpm.dto.DashboardTechCapabilitiesDTO;
import ru.beeline.fdmbpm.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmbpm.dto.bw.BwProductDTO;
import ru.beeline.fdmlib.dto.capability.BusinessCapabilityDTO;
import ru.beeline.fdmlib.dto.capability.TechCapabilityShortDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CapabilityService {

    @Value("${queue.package-queue.name}")
    private String packageQueueName;

    @Autowired
    DashboardClient dashboardClient;

    @Autowired
    CapabilityClient capabilityClient;

    @Autowired
    BWEmployeeClient bwEmployeeClient;

    @Autowired
    PackageClient packageClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityService.class);

    public void sendProduct() {
        try {
            LOGGER.info("sendProduct");
            List<BwProductDTO> products = bwEmployeeClient.getProducts();
            log.info("Receive products:" + products);
            if (products.size() > 0) {
                LOGGER.info("Register package");
                PackageRegistrationResponseDTO responseDTO = packageClient.registerPackage("UPDATE_PRODUCTS", products.size());
                ObjectNode messagePayload = objectMapper.createObjectNode();
                messagePayload.put("packageId", responseDTO.getPackageId());
                messagePayload.put("payload", products.toString());
                LOGGER.info("Send to package-queue");
                sendMessageToCapabilityQueue(packageQueueName, objectMapper.writeValueAsString(messagePayload));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageToCapabilityQueue(String queue, String message) {
        rabbitTemplate.convertAndSend(queue, message, messagePostProcessor -> {
            messagePostProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return messagePostProcessor;
        });
    }

    public Integer sendBusinessCapability() {
        log.info("sendBusinessCapability");
        List<DashboardCapabilityDTO> dashboardCapabilityDTOS = sort(dashboardClient.getCapabilities());
        log.info("Receive Business Capability:" + dashboardCapabilityDTOS);
        List<BusinessCapabilityDTO> businessCapabilityDTOS = capabilityClient.getBusinessCapabilities();
        List<BusinessCapabilityDTO> uniqueBusinessCapability = businessCapabilityDTOS.stream()
                .filter(businessCapability -> dashboardCapabilityDTOS.stream().noneMatch(dashboardCapability ->
                        dashboardCapability.getCode().equals(businessCapability.getCode())))
                .toList();
        log.info("Unique Business Capability:" + uniqueBusinessCapability);
        capabilityClient.deleteBusinessCapabilities(uniqueBusinessCapability);
        return capabilityClient.postBusinessCapabilities(dashboardCapabilityDTOS).getPackageId();
    }

    public Integer sendTechCapability() {
        log.info("sendTechCapability");
        DashboardTechCapabilitiesDTO dashboardTechCapabilitiesDTOList = new DashboardTechCapabilitiesDTO(dashboardClient.getTechCapabilities());
        log.info("Receive Tech Capability:" + dashboardTechCapabilitiesDTOList);
        List<TechCapabilityShortDTO> techCapabilityDTOS = capabilityClient.getTechCapabilities();
        List<TechCapabilityShortDTO> uniqueTechCapability =
                techCapabilityDTOS.stream().filter(techCapability -> dashboardTechCapabilitiesDTOList.getList().stream().
                                noneMatch(dashboardTechCapability -> dashboardTechCapability.getCode().equals(techCapability.getCode())))
                        .toList();
        log.info("Unique Tech Capability:" + uniqueTechCapability);
        capabilityClient.deleteTechCapabilities(uniqueTechCapability);
        return capabilityClient.postTechCapabilities(dashboardTechCapabilitiesDTOList).getPackageId();
    }

    public List<DashboardCapabilityDTO> sort(List<DashboardCapabilityDTO> capabilities) {
        List<DashboardCapabilityDTO> sorted = new ArrayList<>();
        List<DashboardCapabilityDTO> remaining = new ArrayList<>(capabilities);

        sorted.addAll(remaining.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList()));
        remaining.removeAll(sorted);

        int previousSize;
        do {
            previousSize = remaining.size();
            for (DashboardCapabilityDTO capability : new ArrayList<>(remaining)) {
                if (capability.getParent() != null
                        && sorted.stream().anyMatch(c -> capability.getParent().equals(c.getCode()))) {
                    sorted.add(capability);
                    remaining.remove(capability);
                }
            }
        } while (previousSize != remaining.size() && !remaining.isEmpty());

        if (!remaining.isEmpty()) {
            throw new IllegalArgumentException("Для возможностей: " + remaining.stream()
                    .map(DashboardCapabilityDTO::getCode)
                    .collect(Collectors.joining(", ")) + " - не существует указанных родителей");
        }

        return sorted;
    }
}
