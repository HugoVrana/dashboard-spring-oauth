package com.dashboard.oauth.controller.v1;

import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.service.interfaces.IGrantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/grant")
@RequiredArgsConstructor
public class OldGrantController {

    private final IGrantService grantService;

    @PostMapping("/")
    public ResponseEntity<GrantRead> addGrant(@Valid @RequestBody GrantCreate grantCreate) {
        GrantRead createdGrant = grantService.createGrant(grantCreate);
        return ResponseEntity.ok(createdGrant);
    }
}