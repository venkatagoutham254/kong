package aforo.kong.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Let SpringDoc handle its own resources
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://ui.dev.aforo.space",
                        "http://product.dev.aforo.space:8080",
                        "http://metering.dev.aforo.space:8092",
                        "http://usage.dev.aforo.space:8081",
                        "http://ingestion.dev.aforo.space:8088",
                        "http://kong.dev.aforo.space:8086",
                        "http://org.dev.aforo.space:8081",
                        "http://quickbooks.dev.aforo.space:8095",
                        "http://subscription.dev.aforo.space:8084",
                        "http://productscreens.s3-website-ap-northeast-1.amazonaws.com",
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "http://localhost:3002",
                        "http://localhost:3003",
                        "http://localhost:3004",
                        "http://localhost:3005",
                        "http://13.115.248.133",
                        "http://54.238.204.246",
                        "http://18.182.19.181",
                        "http://54.221.164.5"

                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
