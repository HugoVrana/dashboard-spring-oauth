package com.dashboard.oauth.controller.v1;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Void> redirectToSwagger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/swagger-ui/index.html");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
