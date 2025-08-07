package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.AuthSSOClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.StageMapicClient;
import ru.beeline.fdmbpm.dto.mapic.MethodDTO;
import ru.beeline.fdmbpm.dto.mapic.ParameterDTO;
import ru.beeline.fdmlib.dto.product.DiscoveredInterfaceDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MapicSpecService {

    @Autowired
    ProductClient productClient;

    @Autowired
    StageMapicClient stageMapicClient;

    @Autowired
    ObjectMapper objectMapper;

    public void uploadSpec(Integer apiId) {
        DiscoveredInterfaceDTO discoveredInterface = productClient.getInterfaceOperations(apiId);
        String specification = stageMapicClient.getSpecification(discoveredInterface.getApiId());
        try {
            List<MethodDTO> methods = parseOpenApiSpec(specification);
            productClient.updateInterfaceOperations(methods, apiId);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public List<MethodDTO> parseOpenApiSpec(String specJson) throws Exception {
        log.info("specification:" + specJson);
        JsonNode root = objectMapper.readTree(specJson);

        String serverUrl = root.path("servers").get(0).path("url").asText();
        URI uri = new URI(serverUrl);
        String apiContext = uri.getPath();

        List<MethodDTO> methodsList = new ArrayList<>();

        JsonNode pathsNode = root.path("paths");
        Iterator<Map.Entry<String, JsonNode>> pathsIter = pathsNode.fields();
        while (pathsIter.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathsIter.next();
            String pathKey = pathEntry.getKey();
            JsonNode methodsNode = pathEntry.getValue();

            Iterator<Map.Entry<String, JsonNode>> methodsIter = methodsNode.fields();
            while (methodsIter.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodsIter.next();
                String httpMethod = methodEntry.getKey().toUpperCase();
                JsonNode methodDetails = methodEntry.getValue();

                String summary = methodDetails.path("summary").asText();

                String returnType = null;
                JsonNode responses = methodDetails.path("responses");
                Iterator<Map.Entry<String, JsonNode>> respIter = responses.fields();
                while (respIter.hasNext()) {
                    Map.Entry<String, JsonNode> respEntry = respIter.next();
                    if (respEntry.getKey().startsWith("2")) {
                        JsonNode content = respEntry.getValue().path("content");
                        if (content.isObject()) {
                            Iterator<String> contentTypes = content.fieldNames();
                            if (contentTypes.hasNext()) {
                                returnType = contentTypes.next();
                            }
                        }
                        break;
                    }
                }

                List<ParameterDTO> paramList = new ArrayList<>();
                JsonNode paramsNode = methodDetails.path("parameters");
                if (paramsNode.isArray()) {
                    for (JsonNode param : paramsNode) {
                        String paramName = param.path("name").asText();
                        String paramType = param.path("schema").path("type").asText();
                        paramList.add(new ParameterDTO(paramName, paramType));
                    }
                }

                MethodDTO methodDto = new MethodDTO(pathKey, apiContext, summary, httpMethod, returnType, paramList);
                methodsList.add(methodDto);
            }
        }
        return methodsList;
    }
}
