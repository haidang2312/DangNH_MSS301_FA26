package com.fudn.product_service.config;

import com.fudn.product_service.controller.ProductController;
import com.fudn.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SwaggerIntegrationTest.DocumentationApplication.class)
@AutoConfigureMockMvc
class SwaggerIntegrationTest {
    @TestConfiguration
    @EnableAutoConfiguration(excludeName = {"org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration", "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration", "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"})
    @Import({ProductController.class, OpenAPIConfig.class, CorsConfig.class})
    static class DocumentationApplication { }
    @MockitoBean
    private ProductService service;
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
                .andExpect(jsonPath("$.info.title").value("Product Service API"))
                .andExpect(jsonPath("$.info.version").value("v0.0.1"))
                .andExpect(jsonPath("$.paths['/api/products']").exists());
    }

    @Test
    void apiCorsShouldAllowBrowserPreflight() throws Exception {
        mvc.perform(options("/api/products")
                        .header("Origin", "http://localhost:9000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:9000"));
    }
}
