package io.disc99.grpc.spring.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringGRpcClientOnWebFluxApplicationTest {

    @LocalServerPort
    int port;

    @Autowired
    WebTestClient webTestClient;

    @BeforeEach
    public void before() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    public void testEchoWithText() throws Exception {
        this.webTestClient.post()
                .uri("/echo_with_text")
                .bodyValue("message\": \"Hello\"")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("message: \"Hello!\"\n");
    }

    @Test
    public void testEchoWithJson() throws Exception {
        this.webTestClient.post()
                .uri("/echo_with_json")
                .bodyValue("{\"message\":\"Hello\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\n  \"message\": \"Hello\"\n}");
    }

    @Test
    public void testEchoWithProtobufJsonCodec() throws Exception {
        this.webTestClient.post()
                .uri("/echo_with_protobuf_json_codec")
                .contentType(new MediaType("application", "json"))
                .bodyValue("{\"message\":\"Hello\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\n  \"message\": \"Hello\"\n}");
    }
}
