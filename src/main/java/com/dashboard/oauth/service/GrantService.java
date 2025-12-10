package com.dashboard.oauth.service;

import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.service.interfaces.IGrantService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrantService implements IGrantService {

    private final IGrantRepository grantRepository;

    @Override
    public Optional<Grant> getGrantByName(String name) {
        return grantRepository.findByName(name);
    }

    @Override
    public Optional<Grant> getGrantById(ObjectId id) {
        return grantRepository.findById(id);
    }

    @Override
    public Grant createGrant(Grant grant) {
        return grantRepository.save(grant);
    }
}
