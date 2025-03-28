package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.GrafanaClient;
import ru.beeline.fdmlib.dto.techradar.ProcessDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public List<Map<String, String>> getProducts() {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            String response = grafanaClient.getProducts();
            JSONObject jsonResponse = (JSONObject) JSONValue.parse(response);
            JSONObject results = (JSONObject) jsonResponse.get("results");
            JSONObject resultObj = (JSONObject) results.get("A");
            JSONArray frames = (JSONArray) resultObj.get("frames");
            JSONObject firstFrame = (JSONObject) frames.get(0);

            JSONObject schema = (JSONObject) firstFrame.get("schema");
            JSONArray schemafields = (JSONArray) schema.get("fields");
            JSONObject data = (JSONObject) firstFrame.get("data");
            JSONArray values = (JSONArray) data.get("values");

            // Определение индексов столбцов
            int appIndex = -1, epIndex = -1, mnemonicIndex = -1, hostIndex = -1;

            for (int i = 0; i < schemafields.size(); i++) {
                JSONObject field = (JSONObject) schemafields.get(i);
                String fieldName = (String) field.get("name");
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

            // Проверка наличия всех необходимых столбцов
            if (appIndex == -1 || epIndex == -1 || mnemonicIndex == -1 || hostIndex == -1) {
                throw new IllegalArgumentException("Не все необходимые столбцы найдены в ответе.");
            }

            // Формирование итогового массива
            int recordCount = ((JSONArray)values.get(0)).size(); // Количество записей
            for (int i = 0; i < recordCount; i++) {
                Map<String, String> record = new HashMap<>();
                record.put("app", ((JSONArray)values.get(appIndex)).get(i).toString());
                record.put("ep", ((JSONArray)values.get(epIndex)).get(i).toString());
                record.put("mnemonic",((JSONArray)values.get(mnemonicIndex)).get(i).toString());
                record.put("host", ((JSONArray)values.get(hostIndex)).get(i).toString());
                result.add(record);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<Map<String, String>> getMnemonics(ProcessDTO processDTO, List<Map<String, String>> applications) {
        List<Map<String, String>> result = new ArrayList<>();

        try {
            JSONObject jsonResponse = (JSONObject) JSONValue.parse(grafanaClient.getMnemonics(processDTO));
            JSONObject results = (JSONObject) jsonResponse.get("results");
            JSONObject  resultObj = (JSONObject) results.get("A-Instant");
            JSONArray frames = (JSONArray) resultObj.get("frames");
            for (int i = 0; i < frames.size(); i++) {
                JSONObject frame = (JSONObject) frames.get(i);

                JSONObject schema = (JSONObject) frame.get("schema");
                JSONArray fields = (JSONArray) schema.get("fields");

                // Поиск элемента с name = "Value"
                JSONObject valueField = null;
                int valueIndex = -1;
                for (int j = 0; j < fields.size(); j++) {
                    JSONObject field = (JSONObject) fields.get(j);
                    if ("Value".equals(field.get("name"))) {
                        valueField = field;
                        valueIndex = j;
                        break;
                    }
                }

                if (valueField != null) {
                    JSONObject labels = (JSONObject) valueField.get("labels");
                    String host = (String) labels.get("HOST");
                    for (int k = 0; k < applications.size(); k++) {
                        Map<String, String> app = applications.get(k);
                        if (host.equals(app.get("host"))) {
                            // Формирование мнемоники
                            String mnemonic = app.get("mnemonic");
                            String processedMnemonic = mnemonic.substring(mnemonic.indexOf('.') + 1, mnemonic.lastIndexOf('.'));

                            // Формирование объекта результата
                            Map<String, String> record = new HashMap<>();
                            record.put("cmdb_code", "af-messendgers");
                            record.put("proj_lang", "Zabbix");
                            record.put("processed_mnemonic", processedMnemonic);
                            result.add(record);
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
