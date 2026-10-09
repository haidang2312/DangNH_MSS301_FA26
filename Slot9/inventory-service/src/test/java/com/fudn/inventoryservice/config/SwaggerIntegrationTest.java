package com.fudn.inventoryservice.config;

import com.fudn.inventoryservice.controller.InventoryController;
import com.fudn.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SwaggerIntegrationTest.DocumentationApplication.class)
@AutoConfigureMockMvc
class SwaggerIntegrationTest {
    @TestConfiguration
    @EnableAutoConfiguration(excludeName = {"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration", "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration", "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration", "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"})
    @Import({InventoryController.class, OpenAPIConfig.class, CorsConfig.class})
    static class DocumentationApplication { }
    @MockitoBean
    private InventoryService service;
    @Autowired
    private MockMvc mvc;

    @Test
    void swaggerUiShouldBeAccessible() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void apiDocsShouldReturnMetadataAndEndpoints() throws Exception {
        mvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Inventory Service API"))
                .andExpect(jsonPath("$.info.version").value("v0.0.1"))
                .andExpect(jsonPath("$.paths['/api/inventory']").exists());
    }

    @Test
    void apiCorsShouldAllowBrowserPreflight() throws Exception {
        mvc.perform(options("/api/inventory")
                        .header("Origin", "http://localhost:9000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:9000"));
    }
}
