package ru.beeline.fdmbpm.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.ApplicationService;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
@EnableRabbit
public class GraphConsumers {

    @Autowired
    ApplicationService applicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${queue.result-local-graph.name}")
    public void resultLocalGraph(String message) {
        log.info("Received from result-local-graph: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("taskKey") && jsonNode.has("docId")) {
                String taskKey = jsonNode.get("taskKey").asText();

                Map<String, Object> variables = new HashMap<>();
                variables.put("doneLocalGraph", true);

                applicationService.sendMessageToProcess(taskKey, variables);
            } else {
                log.error("Message does not match the required format");
            }
        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    @RabbitListener(queues = "${queue.result-global-graph.name}")
    public void resultGlobalGraph(String message) {
        log.info("Received from result_global_graph: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("taskKey") && jsonNode.has("docId")) {
                String taskKey = jsonNode.get("taskKey").asText();
                Map<String, Object> variables = new HashMap<>();
                variables.put("doneGlobalGraph", true);
                applicationService.sendMessageToProcess(taskKey, variables);
            } else {
                log.error("Message does not match the required format");
            }
        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }
}