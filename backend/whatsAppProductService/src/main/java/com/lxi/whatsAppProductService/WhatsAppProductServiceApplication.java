package com.lxi.whatsAppProductService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = { "com.lxi.whatsAppProductService", "com.fishseller.whatsappservice" })
@EntityScan(basePackages = { "com.fishseller.whatsappservice.model" })
@EnableJpaRepositories(basePackages = { "com.fishseller.whatsappservice.repository" })
public class WhatsAppProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatsAppProductServiceApplication.class, args);
	}

}
