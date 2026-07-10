package com.wayfarer.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

// UserDetailsServiceAutoConfiguration excluded: credential lookup here is
// manual (CredentialRepository + PasswordEncoder), not Spring Security's
// built-in user store — leaving it enabled just prints a throwaway generated
// password to the log on every boot.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableFeignClients
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
