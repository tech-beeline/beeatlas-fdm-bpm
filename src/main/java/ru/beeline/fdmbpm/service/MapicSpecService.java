package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.StageMapicClient;
import ru.beeline.fdmbpm.dto.mapic.MethodDTO;
import ru.beeline.fdmbpm.dto.mapic.ParameterDTO;
import ru.beeline.fdmbpm.dto.wsdlSoap.DefinitionsDTO;
import ru.beeline.fdmbpm.dto.wsdlSoap.OperationDTO;
import ru.beeline.fdmbpm.dto.wsdlSoap.PortDTO;
import ru.beeline.fdmbpm.dto.wsdlSoap.PortTypeDTO;

import java.io.StringReader;
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
        String specification = productClient.getMapicSpec(apiId);
        String normalized = specification.trim();
        try {
            if (normalized.startsWith("<?xml")) {
                List<MethodDTO> soapMethods = parseWsdlSoap(normalized);
                if (!soapMethods.isEmpty()) {
                    productClient.updateInterfaceOperations(soapMethods, apiId);
                }
            } else {
                List<MethodDTO> methods = parseOpenApiSpec(specification);
                productClient.updateInterfaceOperations(methods, apiId);
            }
        } catch (Exception e) {
            log.error("Error while uploading spec", e);
            throw new RuntimeException(e);
        }
    }

    public List<MethodDTO> parseWsdlSoap(String wsdlContent) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(DefinitionsDTO.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        DefinitionsDTO definitions = (DefinitionsDTO) unmarshaller.unmarshal(new StringReader(wsdlContent));
        List<MethodDTO> result = new ArrayList<>();
        String context = getContext(definitions);
        if (definitions.getPortTypes() != null) {
            for (PortTypeDTO portType : definitions.getPortTypes()) {
                if (portType.getOperations() != null) {
                    for (OperationDTO operationDTO : portType.getOperations()) {
                        MethodDTO soapMethod = MethodDTO.builder()
                                .name(operationDTO.getName())
                                .context(context)
                                .type("SOAP")
                                .build();
                        result.add(soapMethod);
                        log.info("SOAP method added to list: {}", soapMethod);
                    }
                }
            }
        }
        if (result.isEmpty()) {
            log.info("SOAP methods list is empty for given WSDL");
        }
        return result;
    }

    private String getContext(DefinitionsDTO definitions) {
        String context = null;
        if (definitions.getService() != null && definitions.getService().getPorts() != null) {
            for (PortDTO port : definitions.getService().getPorts()) {
                if (port.getAddress() != null && port.getAddress().getLocation() != null) {
                    context = port.getAddress().getLocation();
                    break;
                }
            }
        }
        return context;
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
