package ru.beeline.fdmbpm.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.cmdb.CmdbResponseDTO;
import ru.beeline.fdmbpm.dto.cmdb.CmdbResponsibilityDTO;

import java.util.List;

@Slf4j
@Service
public class CmdbClient {
    private final RestTemplate restTemplate;
    private final String cmdbUrl;
    private final String authenticationToken;

    public CmdbClient(@Value("${integration.cmdb-server-url}") String cmdbUrl,
                      @Value("${integration.cmdb-bearer}") String authenticationToken,
                      RestTemplate restTemplate) {
        this.cmdbUrl = cmdbUrl;
        this.restTemplate = restTemplate;
        this.authenticationToken = authenticationToken;
    }

    public CmdbResponseDTO getCmdbInfrastructure(String product) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("AuthenticationToken", "Bearer " + authenticationToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("Request URL: " + cmdbUrl + "/api/cmdb/reports/infrastructure_on_asset_report/" + product + "/simple");
            CmdbResponseDTO cmdbResponseDTO = restTemplate.exchange(cmdbUrl + "/api/cmdb/reports/infrastructure_on_asset_report/" +
                            product + "/simple", HttpMethod.GET, entity,
                    new ParameterizedTypeReference<CmdbResponseDTO>() {
                    }).getBody();

            return cmdbResponseDTO;
        } catch (HttpClientErrorException.Unauthorized e){
            log.info(" 401 : [no body]");
        }
        catch (Exception e) {
            log.error("Error calling CMDB API: " + e.getMessage(), e);
        }
        return null;
    }

    public List<CmdbResponsibilityDTO> getCmdbResponsibilities(String reconciliationId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("AuthenticationToken", "Bearer " + authenticationToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("Request URL: " + cmdbUrl + " /asset/responsibilities/" + reconciliationId);
            return restTemplate.exchange(cmdbUrl + "/asset/responsibilities/" + reconciliationId, HttpMethod.GET, entity,
                                                                    new ParameterizedTypeReference<List<CmdbResponsibilityDTO>>() {
                                                                    }).getBody();

        } catch (HttpClientErrorException.Unauthorized e){
            log.info(" 401 : [no body]");
        }
        catch (Exception e) {
            log.error("Error calling CMDB API: " + e.getMessage(), e);
        }
        return null;
    }
}
