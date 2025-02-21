package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.AuthSSOClient;
import ru.beeline.fdmbpm.client.PackageClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.TechradarClient;
import ru.beeline.fdmbpm.dto.AliasLabelDTO;
import ru.beeline.fdmbpm.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmbpm.dto.techradar.ProductDTO;
import ru.beeline.fdmbpm.gitdomain.FdmGitlabLanguages;
import ru.beeline.fdmbpm.gitrepository.FdmGitlabLanguagesRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RelationsService {
    private static final String OPERATION = "ADD_TECH_PRODUCT_RELATION";

    @Value("${queue.package-queue.name}")
    private String packageQueueName;

    @Autowired
    FdmGitlabLanguagesRepository fdmGitlabLanguagesRepository;

    @Autowired
    TechradarClient techradarClient;

    @Autowired
    ProductClient productClient;

    @Autowired
    PackageClient packageClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AuthSSOClient authSSOClient;

    private ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationsService.class);

    public void createRelations() {
        List<FdmGitlabLanguages> fdmGitlabLanguages = fdmGitlabLanguagesRepository.findUniqueCmdbCodeAndProjLangModify();
        List<ProductDTO> productDTOS = techradarClient.getTech();
        List<AliasLabelDTO> aliasLabelDTOS = productDTOS.stream()
                .flatMap(productDTO -> productDTO.getTech().stream()
                        .map(productTechDTO -> new AliasLabelDTO(
                                productDTO.getProductId(),
                                productDTO.getAlias(),
                                productTechDTO.getId(),
                                productTechDTO.getLabel())))
                .collect(Collectors.toList());
        removeMatchingObjects(aliasLabelDTOS, fdmGitlabLanguages);
        aliasLabelDTOS.forEach(aliasLabelDTO ->
                productClient.deleteRelation(aliasLabelDTO.getTechId(), aliasLabelDTO.getProductId()));
        try {
            LOGGER.info("Register package, operation: {} , fdmGitlabLanguages size: {}", OPERATION, fdmGitlabLanguages.size());
            PackageRegistrationResponseDTO responseDTO = packageClient.registerPackage(
                    OPERATION, fdmGitlabLanguages.size());
            LOGGER.info("the package has been registered, package id: " + responseDTO.getPackageId());
            ObjectNode messagePayload = objectMapper.createObjectNode();
            messagePayload.put("packageId", responseDTO.getPackageId());

            ArrayNode payloadArray = messagePayload.putArray("payload");
            fdmGitlabLanguages.forEach(obj -> {
                ObjectNode item = objectMapper.createObjectNode();
                item.put("proj_lang", obj.getProj_lang());
                item.put("cmdb_code", obj.getCmdb_code());
                payloadArray.add(item);
            });
            LOGGER.info("Send to package-queue");
            sendMessageToCapabilityQueue(packageQueueName, objectMapper.writeValueAsString(messagePayload));
            LOGGER.info("createRelations method completed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageToCapabilityQueue(String queue, String message) {
        rabbitTemplate.convertAndSend(queue, message, messagePostProcessor -> {
            messagePostProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            messagePostProcessor.getMessageProperties().setHeader("Authorization", "Bearer " + authSSOClient.getToken());
            return messagePostProcessor;
        });
    }

    private static void removeMatchingObjects(List<AliasLabelDTO> aliasLabelDTOS, List<FdmGitlabLanguages> fdmGitlabLanguages) {
        Set<Pair<String, String>> fdmGitlabSet = fdmGitlabLanguages.stream()
                .map(fdmGitlabLanguage -> new Pair<>(fdmGitlabLanguage.getProj_lang(), fdmGitlabLanguage.getCmdb_code()))
                .collect(Collectors.toSet());

        aliasLabelDTOS.removeIf(aliasLabelDTO ->
                fdmGitlabSet.contains(new Pair<>(aliasLabelDTO.getLabel(), aliasLabelDTO.getAlias())));

        Set<Pair<String, String>> aliasLabelSet = aliasLabelDTOS.stream()
                .map(aliasLabelDTO -> new Pair<>(aliasLabelDTO.getLabel(), aliasLabelDTO.getAlias()))
                .collect(Collectors.toSet());

        fdmGitlabLanguages.removeIf(fdmGitlabLanguage ->
                aliasLabelSet.contains(new Pair<>(fdmGitlabLanguage.getProj_lang(), fdmGitlabLanguage.getCmdb_code())));
    }
}
