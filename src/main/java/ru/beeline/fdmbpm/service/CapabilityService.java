package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.client.DashboardClient;
import ru.beeline.fdmbpm.dto.DashboardCapabilityDTO;
import ru.beeline.fdmbpm.dto.DashboardTechCapabilitiesDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CapabilityService {

    @Autowired
    DashboardClient dashboardClient;

    @Autowired
    CapabilityClient capabilityClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityService.class);

    public Integer sendBusinessCapability() {
        LOGGER.info("sendBusinessCapability");
        List<DashboardCapabilityDTO> dashboardCapabilityDTOS = sort(dashboardClient.getCapabilities());
        return capabilityClient.postBusinessCapabilities(dashboardCapabilityDTOS).getPackageId();
    }

    public Integer sendTechCapability() {
        LOGGER.info("sendTechCapability");
        List<DashboardTechCapabilitiesDTO> dashboardTechCapabilitiesDTOList = dashboardClient.getTechCapabilities();
        log.info("Receive Tech Capability:" + dashboardTechCapabilitiesDTOList.toString());
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
