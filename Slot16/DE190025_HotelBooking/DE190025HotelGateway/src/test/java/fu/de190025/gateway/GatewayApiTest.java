package fu.de190025.gateway;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApiTest {
  static HttpServer upstream;
  @LocalServerPort int port;
  final HttpClient http = HttpClient.newHttpClient();

  @DynamicPropertySource
  static void routes(DynamicPropertyRegistry props) throws Exception {
    upstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    upstream.createContext(
        "/api/",
        e -> {
          byte[] body = ("{\"path\":\"" + e.getRequestURI().getPath() + "\"}").getBytes();
          e.getResponseHeaders().set("Content-Type", "application/json");
          e.sendResponseHeaders(200, body.length);
          e.getResponseBody().write(body);
          e.close();
        });
    upstream.start();
    for (int i = 0; i < 2; i++) {
      props.add(
          "spring.cloud.gateway.server.webflux.routes[" + i + "].id",
          i == 0 ? () -> "room-service" : () -> "reservation-service");
      props.add(
          "spring.cloud.gateway.server.webflux.routes[" + i + "].predicates[0]",
          i == 0 ? () -> "Path=/api/rooms/**" : () -> "Path=/api/reservations/**");
      props.add(
          "spring.cloud.gateway.server.webflux.routes[" + i + "].uri",
          () -> "http://localhost:" + upstream.getAddress().getPort());
    }
  }

  @AfterAll
  static void stop() {
    if (upstream != null) upstream.stop(0);
  }

  HttpResponse<String> get(String path) throws Exception {
    return http.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void forwardsBothRoutesWithoutRewriting() throws Exception {
    for (String path :
        new String[] {"/api/rooms", "/api/rooms/1", "/api/reservations", "/api/reservations/1"}) {
      var r = get(path);
      assertEquals(200, r.statusCode(), r.body());
      assertTrue(r.body().contains(path));
    }
  }

  @Test
  void swaggerAndCors() throws Exception {
    assertEquals(200, get("/v3/api-docs").statusCode());
    var config = get("/v3/api-docs/swagger-config");
    assertEquals(200, config.statusCode());
    assertTrue(config.body().contains("room-service"));
    assertTrue(config.body().contains("reservation-service"));
    var req =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/reservations"))
            .header("Origin", "http://example.org")
            .header("Access-Control-Request-Method", "POST")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build();
    assertEquals(
        "*",
        http.send(req, HttpResponse.BodyHandlers.ofString())
            .headers()
            .firstValue("access-control-allow-origin")
            .orElse(""));
  }

  @Test
  void permitsPublicJsonWrites() throws Exception {
    for (String method : new String[] {"POST", "PUT", "DELETE"}) {
      var request =
          HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/reservations/1"))
              .header("Content-Type", "application/json")
              .method(method, HttpRequest.BodyPublishers.ofString("{}"))
              .timeout(java.time.Duration.ofSeconds(10))
              .build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode(), response.body());
    }
  }
}
