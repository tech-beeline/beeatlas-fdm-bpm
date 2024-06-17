package ru.beeline.fdmbpm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.client.DashboardClient;

@Component
public class CapabilityService {

    @Autowired
    DashboardClient dashboardClient;

    @Autowired
    CapabilityClient capabilityClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityService.class);

    public Integer sendBusinessCapability() {
        LOGGER.info("sendBusinessCapability");
        return capabilityClient.postBusinessCapabilities(dashboardClient.getCapabilities()).getPackageId();
    }

    public Integer sendTechCapability() {
        LOGGER.info("sendTechCapability");
        return capabilityClient.postTechCapabilities(dashboardClient.getTechCapabilities()).getPackageId();
    }
}
