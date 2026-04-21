package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;
import ru.beeline.fdmbpm.dto.product.NfrCatalogItemDTO;
import ru.beeline.fdmbpm.dto.product.NfrProductItemDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component("" +
        "AssignProductNfrDelegate")
public class AssignProductNfrDelegate implements JavaDelegate {

    @Autowired
    private ProductClient productClient;

    @Override
    public void execute(DelegateExecution execution) {
        String cmdb = (String) execution.getVariable("cmdb");
        log.info("cmdb is {}", cmdb);
        if (cmdb == null || cmdb.isBlank()) {
            log.warn("AssignProductNfrDelegate: cmdb is missing");
            return;
        }

        List<NfrCatalogItemDTO> nfrList = productClient.getAllNfr();

        if (nfrList == null || nfrList.isEmpty()) {
            log.info("NFR catalog is empty or unavailable, skip assignment for {}", cmdb);
            return;
        }
        log.info("NFR catalog size is  {}", nfrList.size());
        AssessmentFitnessForNfrDTO assessment = productClient.getFitnessFunctionsForProduct(cmdb);
        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions =
                assessment != null && assessment.getFitnessFunctions() != null
                        ? assessment.getFitnessFunctions()
                        : List.of();

        List<Integer> desiredIds = new ArrayList<>();

        for (NfrCatalogItemDTO nfr : nfrList) {
            log.info("iterate nfr for {}", nfr.toString());

            Integer nfrId = nfr.getId();
            if (nfrId == null) {
                log.warn("Skip NFR with missing id (code={})", nfr.getCode());
                continue;
            }

            String rule = nfr.getRule();
            if (rule == null || rule.isBlank()) {
                log.info("rule are empty ");
                desiredIds.add(nfrId);
                continue;
            }

            if (!allRuleCodesPassFitness(rule, fitnessFunctions)) {
                log.info("no allRuleCodesPassFitness");
                continue;
            }
            desiredIds.add(nfrId);
        }

        Set<Integer> desiredSet = desiredIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());

        List<NfrProductItemDTO> current = productClient.getProductNfr(cmdb);
        List<NfrProductItemDTO> currentList = current != null ? current : List.of();

        Set<Integer> alreadyAutoAssignedNfrIds = new HashSet<>();
        List<Integer> relationIdsToDelete = new ArrayList<>();

        for (NfrProductItemDTO item : currentList) {
            if (item == null || item.getId() == null) {
                continue;
            }
            String sourcePurpose = item.getSourcePurpose();
            boolean isAuto = sourcePurpose != null && sourcePurpose.equalsIgnoreCase("Beeatlas");
            if (!isAuto) {
                continue;
            }
            alreadyAutoAssignedNfrIds.add(item.getId());
            if (!desiredSet.contains(item.getId())) {
                if (item.getRelationId() != null) {
                    relationIdsToDelete.add(item.getRelationId());
                } else {
                    log.warn("Auto-assigned NFR {} has no relationId in response; can't delete", item.getId());
                }
            }
        }

        if (!relationIdsToDelete.isEmpty()) {
            productClient.deleteBeeatlasProductNfrRelations(cmdb, relationIdsToDelete);
            log.info("Deleted {} obsolete Beeatlas NFR relations for product {}", relationIdsToDelete.size(), cmdb);
        }

        List<Integer> idsToAssign = desiredIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(id -> !alreadyAutoAssignedNfrIds.contains(id))
                .toList();

        if (idsToAssign.isEmpty()) {
            log.info("No new NFR to assign for product {} (desired={}, alreadyAutoAssigned={})",
                    cmdb, desiredSet.size(), alreadyAutoAssignedNfrIds.size());
            return;
        }

        productClient.postProductNfr(cmdb, idsToAssign);
        log.info("Posted {} NFR ids to product {}", idsToAssign.size(), cmdb);
    }

    private static boolean allRuleCodesPassFitness(String rule,
                                                   List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions) {
        String normalized = rule.replaceAll("\\s+", "");
        String[] parts = normalized.split(",", -1);
        boolean sawNonEmptyToken = false;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sawNonEmptyToken = true;
            AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO ff = findByCodeIgnoreCase(fitnessFunctions, part);
            if (ff == null || !Boolean.TRUE.equals(ff.getIsCheck())) {
                return false;
            }
        }
        return sawNonEmptyToken;
    }

    private static AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO findByCodeIgnoreCase(
            List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions,
            String code) {
        for (AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO ff : fitnessFunctions) {
            if (ff.getCode() != null && ff.getCode().equalsIgnoreCase(code)) {
                return ff;
            }
        }
        return null;
    }
}
