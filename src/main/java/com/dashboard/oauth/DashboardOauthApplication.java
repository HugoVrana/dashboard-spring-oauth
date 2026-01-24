package com.dashboard.oauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableScheduling
public class DashboardOauthApplication {

    @GetMapping("/")
    public String home() {
        return "Spring Dashboard is here!";
    }

    public static void main(String[] args) {
        SpringApplication.run(DashboardOauthApplication.class, args);
    }

}
