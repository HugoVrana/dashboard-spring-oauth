package com.dashboard.oauth.controller;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.service.interfaces.IGrantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/grant")
@RequiredArgsConstructor
public class GrantController {

    private final IGrantService grantService;
    private final IGrantMapper grantMapper;

    @PostMapping("/")
    public ResponseEntity<GrantRead> addGrant(@Valid @RequestBody GrantCreate grantCreate) {
        Optional<Grant> grant = grantService.getGrantByName(grantCreate.getName());
        if (grant.isPresent()) {
            throw new ConflictException("Grant already exists");
        }

        Grant g = new Grant();
        g.setName(grantCreate.getName());
        g.setDescription(grantCreate.getDescription());
        Audit a = new Audit();
        a.setCreatedAt(Instant.now());
        g.setAudit(a);
        g = grantService.createGrant(g);
        GrantRead grantRead = grantMapper.toRead(g);
        return ResponseEntity.ok(grantRead);
    }

}
