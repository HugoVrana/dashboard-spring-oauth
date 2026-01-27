package com.dashboard.oauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DashboardOauthApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardOauthApplication.class, args);
    }
}
