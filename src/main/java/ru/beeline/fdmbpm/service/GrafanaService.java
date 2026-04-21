package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.GrafanaClient;
import ru.beeline.fdmbpm.dto.techradar.ProcessDTO;

import java.util.*;

@Slf4j
@Component
public class GrafanaService {

    @Autowired
    GrafanaClient grafanaClient;

    @Autowired
    ObjectMapper objectMapper;

    public List<String> getProcessList() {
        List<String> processValues = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(grafanaClient.getProcessList());
            JsonNode listNode = root.path("dashboard")
                    .path("templating")
                    .path("list");

            for (JsonNode node : listNode) {
                if (node.has("name") && node.get("name").asText().equals("processes")) {
                    JsonNode optionsNode = node.path("options");
                    for (JsonNode option : optionsNode) {
                        if (option.has("value")) {
                            processValues.add(option.get("value").asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка разбора JSON: ", e);
        }
        return processValues;
    }

    public Map<String, Map<String, String>> getProducts() {
        Map<String, Map<String, String>> result = new HashMap<>();
        try {
            String response = grafanaClient.getProducts();
            JsonNode root = objectMapper.readTree(response);

            JsonNode results = root.path("results");
            JsonNode resultObj = results.path("A");
            JsonNode frames = resultObj.path("frames");
            JsonNode firstFrame = frames.get(0);

            JsonNode schema = firstFrame.path("schema");
            JsonNode schemafields = schema.path("fields");
            JsonNode data = firstFrame.path("data");
            JsonNode values = data.path("values");

            int appIndex = -1, epIndex = -1, mnemonicIndex = -1, hostIndex = -1;

            for (int i = 0; i < schemafields.size(); i++) {
                JsonNode field = schemafields.get(i);
                String fieldName = field.path("name").asText();
                switch (fieldName) {
                    case "Приложение":
                        appIndex = i;
                        break;
                    case "ЭП":
                        epIndex = i;
                        break;
                    case "mnemonics":
                        mnemonicIndex = i;
                        break;
                    case "HOST":
                        hostIndex = i;
                        break;
                }
            }

            if (appIndex == -1 || epIndex == -1 || mnemonicIndex == -1 || hostIndex == -1) {
                throw new IllegalArgumentException("Не все необходимые столбцы найдены в ответе.");
            }

            int recordCount = values.get(0).size();
            for (int i = 0; i < recordCount; i++) {
                Map<String, String> record = new HashMap<>();
                record.put("app", values.get(appIndex).get(i).asText());
                record.put("ep", values.get(epIndex).get(i).asText());
                record.put("mnemonic", values.get(mnemonicIndex).get(i).asText());
                record.put("host", values.get(hostIndex).get(i).asText());
                result.put(values.get(hostIndex).get(i).asText(), record);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Set<Map<String, String>> getMnemonics(ProcessDTO processDTO, Map<String, Map<String, String>> applications) {
        Set<Map<String, String>> result = new HashSet<>();
        try {
            String jsonString = grafanaClient.getMnemonics(processDTO).trim();
            JsonNode root = objectMapper.readTree(jsonString);

            JsonNode results = root.path("results");
            JsonNode resultObj = results.path("A-Instant");
            JsonNode frames = resultObj.path("frames");

            if (frames.isArray()) {
                for (JsonNode frame : frames) {
                    JsonNode schema = frame.path("schema");
                    JsonNode fields = schema.path("fields");

                    JsonNode valueField = null;
                    for (int j = 0; j < fields.size(); j++) {
                        JsonNode field = fields.get(j);
                        if ("Value".equals(field.path("name").asText())) {
                            valueField = field;
                            break;
                        }
                    }

                    if (valueField != null) {
                        JsonNode labels = valueField.path("labels");
                        String host = labels.path("HOST").asText();
                        if (applications.containsKey(host) && applications.get(host).containsKey("mnemonic")) {
                            String mnemonic = applications.get(host).get("mnemonic");
                            String processedMnemonic = mnemonic.substring(
                                    mnemonic.indexOf('.') + 1,
                                    mnemonic.lastIndexOf('.')
                            );
                            Map<String, String> record = new HashMap<>();
                            record.put("cmdb_code", processedMnemonic);
                            record.put("proj_lang", labels.get("groupname").asText());
                            result.add(record);
                        }
                    }
                }
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
