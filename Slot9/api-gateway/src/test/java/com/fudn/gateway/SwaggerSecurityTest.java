package com.fudn.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ApiGatewayApplication.class)
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
    "services.product.url", "services.order.url", "services.inventory.url"}))
class SwaggerSecurityTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void swaggerUiAndConfigurationShouldBePublic() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls.length()").value(3))
                .andExpect(jsonPath("$.urls[*].name").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "Product Service", "Order Service", "Inventory Service")))
                .andExpect(jsonPath("$.urls[*].url").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "/aggregate/product-service/v3/api-docs",
                        "/aggregate/order-service/v3/api-docs",
                        "/aggregate/inventory-service/v3/api-docs")));
    }

    @Test
    void allAggregateRoutesShouldRewritePathWithoutToken() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api-docs"))
                .willReturn(WireMock.okJson("{\"openapi\":\"3.0.1\"}")));
        for (String service : new String[]{"product", "order", "inventory"}) {
            mvc.perform(get("/aggregate/" + service + "-service/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openapi").value("3.0.1"));
        }
        WireMock.verify(3, WireMock.getRequestedFor(WireMock.urlEqualTo("/api-docs")));
    }

    @Test
    void protectedApiShouldRequireToken() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/order")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/inventory")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightShouldBePublic() throws Exception {
        mvc.perform(options("/api/products")
                        .header("Origin", "http://localhost:9000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}
