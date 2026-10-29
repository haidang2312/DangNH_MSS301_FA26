package fu.de190025.gateway.config;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.*;
@Configuration
public class CorsConfig {
 @Bean public CorsWebFilter corsWebFilter() {
  CorsConfiguration c=new CorsConfiguration(); c.addAllowedOrigin("*"); c.addAllowedMethod("*"); c.addAllowedHeader("*");
  UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",c); return new CorsWebFilter(source);
 }
}
