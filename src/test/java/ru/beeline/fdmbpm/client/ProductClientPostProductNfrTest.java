package ru.beeline.fdmbpm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ProductClientPostProductNfrTest {

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
    void postsAlias() {
        server.expect(requestTo(BASE_URL + "/api/v1/nfr/product?alias=BC-1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0]").value(11))
                .andRespond(withStatus(HttpStatus.OK));

        productClient.postProductNfr("BC-1", List.of(11));

        server.verify();
    }

    @Test
    void aliasWithSpace_isEncodedOnce() {
        server.expect(requestTo(BASE_URL + "/api/v1/nfr/product?alias=td%20pm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        productClient.postProductNfr("td pm", List.of(11));

        server.verify();
    }
}
