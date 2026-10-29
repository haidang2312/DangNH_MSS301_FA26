package fu.de190025.room;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.*;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:rooms;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class RoomApiTest {
  @LocalServerPort int port;
  final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  final ObjectMapper json = new ObjectMapper();

  HttpResponse<String> request(String method, String path, String body) throws Exception {
    var b =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15));
    return http.send(
        b.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  JsonNode check(HttpResponse<String> r, int code, int status) throws Exception {
    assertEquals(code, r.statusCode(), r.body());
    var n = json.readTree(r.body());
    assertEquals(status, n.path("status").asInt(), r.body());
    assertDoesNotThrow(() -> java.time.Instant.parse(n.path("timestamp").asText()));
    return n.path("data");
  }

  @Test
  void lifecycleAndValidation() throws Exception {
    String payload =
        """
{"roomNumber":"creator_a","roomType":"DOUBLE","pricePerNight":500000,"capacity":3,"floor":1,"status":"OCCUPIED"}
""";
    var created = check(request("POST", "/api/rooms", payload), 201, 1);
    long id = created.path("roomId").asLong();
    assertEquals("AVAILABLE", created.path("status").asText());
    check(request("POST", "/api/rooms", payload), 226, 3);
    check(
        request(
            "POST", "/api/rooms", payload.replace("creator_a", "creator_b").replace("500000", "0")),
        406,
        2);
    check(
        request(
            "POST",
            "/api/rooms",
            payload.replace("creator_a", "creator_c").replace("DOUBLE", "INVALID")),
        406,
        2);
    check(request("POST", "/api/rooms", "{}"), 406, 2);
    check(request("GET", "/api/rooms/999999", null), 404, 4);
    check(request("PUT", "/api/rooms/" + id, "{\"floor\":2}"), 200, 1);
    check(request("PUT", "/api/rooms/" + id, "{\"floor\":0}"), 406, 2);
    assertEquals(2, check(request("GET", "/api/rooms/" + id, null), 200, 1).path("floor").asInt());
    var page =
        check(request("GET", "/api/rooms?roomType=DOUBLE&status=AVAILABLE&size=1", null), 200, 1);
    assertEquals(1, page.path("totalElements").asInt());
    check(request("GET", "/api/rooms?size=101", null), 406, 2);
    check(request("GET", "/api/rooms?page=-1", null), 406, 2);
    check(request("GET", "/api/rooms?roomType=INVALID", null), 406, 2);
    assertTrue(check(request("DELETE", "/api/rooms/" + id, null), 200, 1).isNull());
    assertEquals(
        "MAINTENANCE",
        check(request("GET", "/api/rooms/" + id, null), 200, 1).path("status").asText());
  }

  @Test
  void swaggerAndCors() throws Exception {
    assertEquals(200, request("GET", "/v3/api-docs", null).statusCode());
    var req =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/rooms"))
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
}
