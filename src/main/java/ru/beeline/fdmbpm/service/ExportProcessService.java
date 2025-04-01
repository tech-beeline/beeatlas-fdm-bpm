package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.client.NotifyServiceClient;
import ru.beeline.fdmbpm.client.TechradarClient;
import ru.beeline.fdmbpm.dto.DocIdDTO;

@Slf4j
@Component
public class ExportProcessService {

    @Autowired
    TechradarClient techradarClient;

    @Autowired
    CapabilityClient capabilityClient;

    @Autowired
    NotifyServiceClient notifyServiceClient;

    public void processExportingToExcel(String entityType, Integer docId) {
        DocIdDTO docIdDTO;
        switch (entityType) {
            case "tech":
                docIdDTO = techradarClient.postExportTech(docId);
                if (docIdDTO != null) {
                    log.info("tech" + docIdDTO);
                }
                break;
            case "tech-capability":
                docIdDTO = capabilityClient.postExportCapability(docId, "tech-capability");
                if (docIdDTO != null) {
                    log.info("tech-capability" + docIdDTO);
                }
                break;
            case "business-capability":
                docIdDTO = capabilityClient.postExportCapability(docId, "business-capability");
                if (docIdDTO != null) {
                    log.info("business-capability" + docIdDTO);
                }
                break;
        }
    }

    public void generationNotification(String entityType, Integer docId, Integer userId) {
        notifyServiceClient.postExportNotify(docId, entityType, userId);
    }
}
