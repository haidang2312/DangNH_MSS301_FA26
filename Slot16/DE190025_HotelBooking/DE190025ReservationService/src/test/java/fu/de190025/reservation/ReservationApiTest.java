package fu.de190025.reservation;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.*;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:reservations;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class ReservationApiTest {
  static HttpServer rooms;
  static volatile int roomHttp = 200;
  static volatile String roomStatus = "AVAILABLE";
  static volatile int rate = 500000;

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry props) throws Exception {
    rooms = HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
    rooms.createContext(
        "/api/rooms/",
        e -> {
          String body =
              "{\"status\":1,\"data\":{\"roomId\":1,\"roomNumber\":\"A-101\",\"roomType\":\"DOUBLE\",\"pricePerNight\":"
                  + rate
                  + ",\"capacity\":3,\"floor\":1,\"status\":\""
                  + roomStatus
                  + "\"}}";
          byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
          e.getResponseHeaders().set("Content-Type", "application/json");
          e.sendResponseHeaders(roomHttp, bytes.length);
          e.getResponseBody().write(bytes);
          e.close();
        });
    rooms.start();
    props.add("room.service.url", () -> "http://localhost:" + rooms.getAddress().getPort());
  }

  @AfterAll
  static void stop() {
    if (rooms != null) rooms.stop(0);
  }

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

  String payload =
      """
{"guestName":"Shop ABC","guestEmail":"ads@example.com","guestPhone":"0900000000","checkInDate":"25/07/2026","checkOutDate":"28/07/2026","numberOfGuests":2,"roomId":1}
""";

  @Test
  void bookingRulesAndLifecycle() throws Exception {
    var created = check(request("POST", "/api/reservations", payload), 201, 1);
    long id = created.path("reservationId").asLong();
    assertEquals("CONFIRMED", created.path("status").asText());
    assertEquals(1500000, created.path("totalAmount").asInt());
    assertEquals(
        "A-101",
        check(request("GET", "/api/reservations/" + id, null), 200, 1)
            .path("room")
            .path("roomNumber")
            .asText());
    roomHttp = 404;
    check(request("POST", "/api/reservations", payload), 404, 4);
    roomHttp = 500;
    check(request("POST", "/api/reservations", payload), 500, 0);
    roomHttp = 200;
    roomStatus = "OCCUPIED";
    check(request("POST", "/api/reservations", payload), 400, 5);
    roomStatus = "MAINTENANCE";
    check(request("POST", "/api/reservations", payload), 400, 5);
    roomStatus = "AVAILABLE";
    check(
        request("POST", "/api/reservations", payload.replace("28/07/2026", "25/07/2026")), 406, 2);
    check(
        request(
            "POST",
            "/api/reservations",
            payload.replace("numberOfGuests\":2", "numberOfGuests\":4")),
        406,
        2);
    check(request("POST", "/api/reservations", payload.replace("ads@example.com", "bad")), 406, 2);
    check(
        request("POST", "/api/reservations", payload.replace("25/07/2026", "31/02/2026")), 406, 2);
    check(request("PUT", "/api/reservations/" + id, "{\"checkOutDate\":\"24/07/2026\"}"), 406, 2);
    assertEquals(
        "28/07/2026",
        check(request("GET", "/api/reservations/" + id, null), 200, 1)
            .path("checkOutDate")
            .asText());
    rate = 600000;
    assertEquals(
        1800000,
        check(request("PUT", "/api/reservations/" + id, "{\"guestName\":\"Updated Shop\"}"), 200, 1)
            .path("totalAmount")
            .asInt());
    assertEquals(
        1,
        check(request("GET", "/api/reservations?guestName=Updated&status=CONFIRMED", null), 200, 1)
            .path("totalElements")
            .asInt());
    check(request("GET", "/api/reservations?size=101", null), 406, 2);
    check(request("GET", "/api/reservations?status=INVALID", null), 406, 2);
    assertTrue(check(request("DELETE", "/api/reservations/" + id, null), 200, 1).isNull());
    assertEquals(
        "CANCELLED",
        check(request("GET", "/api/reservations/" + id, null), 200, 1).path("status").asText());
    check(request("GET", "/api/reservations/99999", null), 404, 4);
    check(request("PUT", "/api/reservations/99999", "{}"), 404, 4);
    check(request("DELETE", "/api/reservations/99999", null), 404, 4);
  }

  @Test
  void swagger() throws Exception {
    assertEquals(200, request("GET", "/v3/api-docs", null).statusCode());
  }
}
