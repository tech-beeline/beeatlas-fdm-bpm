/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.techradar.ProcessDTO;


@Slf4j
@Service
public class GrafanaClient {

    RestTemplate restTemplate;

    private final String grafanaServerUrl;
    private final String bearerToken;
    private final String requestRow = "{\n" +
            "    \"queries\": [\n" +
            "        {\n" +
            "            \"refId\": \"A\",\n" +
            "            \"datasource\": {\n" +
            "                \"type\": \"mssql\",\n" +
            "                \"uid\": \"yDWtMbsGk\"\n" +
            "            },\n" +
            "            \"rawSql\": \"SELECT dbo.CI_Applications.name as [Приложение]\\r\\n     , dbo.CI_AppInstances.Name AS [ЭП]\\r\\n     , dbo.CI_AppInstances.mnemonics AS [mnemonics]\\r\\n     , dbo.CI_ServerDomain.Name AS HOST\\r\\n--     , '' AS GROUP_EMPTY\\r\\nFROM            dbo.CI_ServerDomain INNER JOIN\\r\\n                         dbo.Links_CMDBClassesLinks INNER JOIN\\r\\n                         dbo.CI_AppInstances INNER JOIN\\r\\n                         dbo.CI_Applications INNER JOIN\\r\\n                         dbo.Links_CMDBClassesLinks AS Links_CMDBClassesLinks_1 ON dbo.CI_Applications.ID_Ekz = Links_CMDBClassesLinks_1.ID_Ekz2 ON dbo.CI_AppInstances.ID_Ekz = Links_CMDBClassesLinks_1.ID_Ekz1 ON \\r\\n--                         dbo.Links_CMDBClassesLinks.ID_Ekz2 = dbo.CI_AppInstances.ID_Ekz ON dbo.CI_ServerDomain.ID_Ekz = dbo.Links_CMDBClassesLinks.ID_Ekz1 LEFT OUTER JOIN\\r\\n--                         dbo.mon_unix_info ON dbo.CI_ServerDomain.Name = dbo.mon_unix_info.sysname\\r\\n                         dbo.Links_CMDBClassesLinks.ID_Ekz2 = dbo.CI_AppInstances.ID_Ekz ON dbo.CI_ServerDomain.ID_Ekz = dbo.Links_CMDBClassesLinks.ID_Ekz1 -- перенесено из переменной\\r\\nWHERE        (dbo.CI_ServerDomain.NSM_Status = 'В эксплуатации') AND (dbo.CI_AppInstances.NSM_Status = 'В эксплуатации') AND (dbo.CI_Applications.NSM_Status = 'В эксплуатации') AND \\r\\n                         (Links_CMDBClassesLinks_1.Status = 'Новый') AND (dbo.Links_CMDBClassesLinks.Status = 'Новый') \\r\\nAND dbo.CI_ServerDomain.DomainType_2 not in ('namespace', 'alias')\\r\\n--AND dbo.CI_ServerDomain.Name IN ($instance + '')\\r\\nORDER by HOST\",\n" +
            "            \"format\": \"table\",\n" +
            "            \"datasourceId\": 39,\n" +
            "            \"intervalMs\": 60000,\n" +
            "            \"maxDataPoints\": 833\n" +
            "        }\n" +
            "    ],\n" +
            "    \"from\": \"now-15m\",\n" +
            "    \"to\": \"now\"\n" +
            "} ";

    public GrafanaClient(@Value("${integration.grafana-server-url}") String grafanaServerUrl,
                         @Value("${integration.grafana-bearer}") String bearerToken,
                         RestTemplate restTemplate) {
        this.grafanaServerUrl = grafanaServerUrl;
        this.bearerToken = bearerToken;
        this.restTemplate = restTemplate;
    }

    public String getProcessList() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + bearerToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    grafanaServerUrl + "/api/dashboards/uid/jwCtre6Sz",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            String responseBody = response.getBody();
            return responseBody;

        } catch (Exception e) {
            log.error("Ошибка получения данных: ", e);
            return null;
        }
    }

    public String getProducts() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + bearerToken);

            HttpEntity<String> entity = new HttpEntity<>(requestRow, headers);

            return restTemplate.exchange(grafanaServerUrl + "/api/ds/query",
                    HttpMethod.POST, entity, String.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public String getMnemonics(ProcessDTO processDTO) {
        try {

            String mnemonicsRequestRow = "{\n" +
                    "    \"queries\": [\n" +
                    "        {\n" +
                    "            \"refId\": \"A-Instant\",\n" +
                    "            \"datasource\": {\n" +
                    "                \"type\": \"prometheus\",\n" +
                    "                \"uid\": \"ru1LRS-Ik\"\n" +
                    "            },\n" +
                    "            \"editorMode\": \"code\",\n" +
                    "            \"expr\": \"label_replace(\\r\\n    namedprocess_namegroup_num_procs{groupname=~\\\"%s\\\"}\\r\\n, \\\"HOST\\\", \\\"$1\\\", \\\"instance\\\", \\\"^(.*):.*$\\\")\",\n" +
                    "            \"legendFormat\": \"__auto\",\n" +
                    "            \"range\": false,\n" +
                    "            \"instant\": true\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"from\": \"now-15m\",\n" +
                    "    \"to\": \"now\"\n" +
                    "}";
            String body = String.format(
                    mnemonicsRequestRow,
                    processDTO.getProcess()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + bearerToken);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            return restTemplate.exchange(grafanaServerUrl + "/api/ds/query",
                    HttpMethod.POST, entity, String.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}