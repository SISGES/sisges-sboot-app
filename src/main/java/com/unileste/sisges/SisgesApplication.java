package com.unileste.sisges;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SisgesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SisgesApplication.class, args);
	}

}
