package com.example.react.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	@PostConstruct
	public void init() {
		System.out.println("CORS Config Initialized");
	}
	@Override
	public void addCorsMappings(final CorsRegistry registry) {
		registry.addMapping("/**").allowedOrigins("*") // ✅ Origen permitido
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*"); // Si

	}
}