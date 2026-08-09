package ru.iskandar.quizandar.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI quizandarOpenAPI() {
		return new OpenAPI().info(new Info().title("Quizandar API")
				.description("Интерактивная викторина. Документация REST-эндпоинтов.").version("1.0.0")

		);
	}
}
