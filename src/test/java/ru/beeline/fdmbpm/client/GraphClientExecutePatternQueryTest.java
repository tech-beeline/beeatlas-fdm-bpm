package ru.beeline.fdmbpm.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GraphClientExecutePatternQueryTest {

    private static final String BASE_URL = "https://graph.example.com";

    private GraphClient graphClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        graphClient = new GraphClient(BASE_URL, restTemplate);
    }

    @Test
    void putsCypherIntoHeaderAsSingleLine() {
        server.expect(requestTo(BASE_URL + "/api/v1/elements"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("CYPHER-QUERY", "g.V() .has('a','BC-1') .has('b','BC-1')"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        assertThat(graphClient.executePatternQuery("g.V()\n.has('a','BC-1')\r\n.has('b','BC-1')"))
                .isEmpty();

        server.verify();
    }
}
