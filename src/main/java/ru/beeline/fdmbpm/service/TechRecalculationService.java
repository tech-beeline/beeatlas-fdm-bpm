package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.dto.capability.TechCapabilityShortDTO;

import java.util.List;

@Slf4j
@Component
public class TechRecalculationService {

    @Value("${rabbitmq.fanout-exchange}")
    private String fanoutExchange;

    @Autowired
    CapabilityClient capabilityClient;

    @Autowired
    RabbitService rabbitService;

    @Autowired
    ObjectMapper objectMapper = new ObjectMapper();

    public void getTechRecalculation() {
        List<TechCapabilityShortDTO> techCapabilityShortDTOList = capabilityClient.getTechCapabilities().stream()
                .filter(techCapabilityShortDTO -> techCapabilityShortDTO.getDeletedDate() == null).toList();
        log.info("Send to package-queue");
        log.info("Sending {} items to package-queue: {}", techCapabilityShortDTOList.size(), fanoutExchange);
        techCapabilityShortDTOList.forEach(obj -> {
            ObjectNode messagePayload = objectMapper.createObjectNode();
            messagePayload.put("changeType", "UPDATE");
            messagePayload.put("name", obj.getName());
            messagePayload.put("entityId", obj.getId());
            try {
                rabbitService.sendToFanoutTcExchange(fanoutExchange, objectMapper.writeValueAsString(messagePayload));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("method completed");
    }
}
