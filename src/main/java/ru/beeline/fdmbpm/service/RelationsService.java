package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.beeline.fdmbpm.client.PackageClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.TechradarClient;
import ru.beeline.fdmbpm.dto.FdmGitlabLanguagesDTO;
import ru.beeline.fdmbpm.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmbpm.dto.techradar.ProductDTO;
import ru.beeline.fdmbpm.dto.techradar.ProductTechDTO;
import ru.beeline.fdmbpm.gitdomain.FdmGitlabLanguages;
import ru.beeline.fdmbpm.gitrepository.RingRepository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RelationsService {
    private static final String OPERATION = "ADD_TECH_PRODUCT_RELATION";

    @Value("${queue.package-queue.name}")
    private String packageQueueName;

    @Autowired
    RingRepository ringRepository;

    @Autowired
    TechradarClient techradarClient;

    @Autowired
    ProductClient productClient;

    @Autowired
    PackageClient packageClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationsService.class);

    public void createRelations() {
        List<FdmGitlabLanguages> fdmGitlabLanguages = ringRepository.findUniqueCmdbCodeAndProjLang();
        Map<String, Pair<List<String>, ProductDTO>> productPairMap = new HashMap<>();
        List<ProductDTO> productDTOS = techradarClient.getTech();
        productDTOS.forEach(productDTO -> productPairMap.put(productDTO.getAlias(), new Pair<>(productDTO.getTech().stream().map(ProductTechDTO::getLabel).collect(Collectors.toList()), productDTO)));
        Iterator<FdmGitlabLanguages> iterator = fdmGitlabLanguages.iterator();
        while (iterator.hasNext()) {
            FdmGitlabLanguages pair = iterator.next();
            if (productPairMap.containsKey(pair.getCmdb_code()) &&
                    productPairMap.get(pair.getCmdb_code()).a.contains(pair.getProj_lang())) {
                iterator.remove();
            } else {
                Integer techId = productPairMap.get(pair.getCmdb_code())
                        .b.getTech().stream()
                        .filter(tech -> tech.getLabel().equals(pair.getProj_lang()))
                        .findFirst()
                        .get().getId();
                Integer productId = productPairMap.get(pair.getCmdb_code()).b.getProductId();
                productClient.deleteRelation(techId, productId);
            }
        }
        try {
            LOGGER.info("Register package");
            PackageRegistrationResponseDTO responseDTO = packageClient.registerPackage(
                    OPERATION, fdmGitlabLanguages.size());
            ObjectNode messagePayload = objectMapper.createObjectNode();
            messagePayload.put("packageId", responseDTO.getPackageId());
            messagePayload.put("payload", fdmGitlabLanguages.stream().map(obj ->
                            FdmGitlabLanguagesDTO.builder().proj_lang(obj.getProj_lang()).cmdb_code(obj.getCmdb_code()).build())
                    .collect(Collectors.toList()
                    ).toString());
            LOGGER.info("Send to package-queue");
            sendMessageToCapabilityQueue(packageQueueName, objectMapper.writeValueAsString(messagePayload));
        } catch (
                Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageToCapabilityQueue(String queue, String message) {
        rabbitTemplate.convertAndSend(queue, message, messagePostProcessor -> {
            messagePostProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return messagePostProcessor;
        });
    }
}
