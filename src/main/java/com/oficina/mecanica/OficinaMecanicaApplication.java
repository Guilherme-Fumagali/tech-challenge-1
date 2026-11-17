package com.oficina.mecanica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OficinaMecanicaApplication {
    public static void main(String[] args) {
        SpringApplication.run(OficinaMecanicaApplication.class, args);
    }
}
