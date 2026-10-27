package com.kunaldo.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


//@EnableEurekaClient is deprecated no need to annotate at springboot main applicationit is fine if
//		we add the spring-cloud-starter-netflix-eureka-client dependency in pom and if we have the
//		application name in yml or properties file it will be registered to Eureka Server

@SpringBootApplication
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

}
