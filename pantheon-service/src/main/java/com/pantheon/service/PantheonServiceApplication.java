package com.pantheon.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PantheonServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PantheonServiceApplication.class, args);
	}

}
