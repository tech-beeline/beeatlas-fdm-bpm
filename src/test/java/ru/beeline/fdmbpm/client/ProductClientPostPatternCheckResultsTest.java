package ru.beeline.fdmbpm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ProductClientPostPatternCheckResultsTest {

    private static final String BASE_URL = "https://products.example.com";

    private ProductClient productClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        productClient = new ProductClient(BASE_URL, restTemplate, restTemplate, new ObjectMapper());
    }

    @Test
    void pipeline_postsV2WithBranchAndSourceId() {
        server.expect(requestTo(BASE_URL + "/api/v2/product/BC-1/patterns/pipeline?branch=feature&source-id=77"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value("P-1"))
                .andExpect(jsonPath("$[0].isCheck").value(true))
                .andRespond(withStatus(HttpStatus.CREATED));

        productClient.postPatternCheckResults("BC-1", "pipeline", 77, "feature", List.of(
                PatternCheckResultDTO.builder().code("P-1").isCheck(true).build()));

        server.verify();
    }

    @Test
    void nfr_postsV2WithBranchWithoutSourceId() {
        server.expect(requestTo(BASE_URL + "/api/v2/product/BC-1/patterns/nfr?branch=main"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        productClient.postPatternCheckResults("BC-1", "nfr", null, "main", List.of(
                PatternCheckResultDTO.builder().code("P-2").isCheck(false).build()));

        server.verify();
    }

    @Test
    void blankBranch_defaultsToMain() {
        server.expect(requestTo(BASE_URL + "/api/v2/product/BC-1/patterns/pipeline?branch=main&source-id=1"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        productClient.postPatternCheckResults("BC-1", "pipeline", 1, "  ", List.of(
                PatternCheckResultDTO.builder().code("P-3").isCheck(true).build()));

        server.verify();
        assertThat(ProductClient.normalizeBranch(null)).isEqualTo("main");
    }
}
