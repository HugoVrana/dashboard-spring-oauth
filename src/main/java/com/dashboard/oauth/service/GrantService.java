package com.dashboard.oauth.service;

import com.dashboard.common.model.ActivityEvent;
import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.utility.diff.DiffComparer;
import com.dashboard.common.utility.diff.DiffResult;
import com.dashboard.oauth.authentication.GrantsAuthentication;
import com.dashboard.oauth.context.DiffContext;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.enums.ActivityEventType;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IGrantService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Scope("singleton")
@RequiredArgsConstructor
public class GrantService implements IGrantService {

    private final IGrantRepository grantRepository;
    private final IActivityFeedService activityFeedService;
    private final IGrantMapper grantMapper;

    @Override
    public Optional<Grant> getGrantByName(String name) {
        return grantRepository.findByName(name);
    }

    @Override
    public Optional<Grant> getGrantById(ObjectId id) {
        return grantRepository.findById(id);
    }

    @Override
    public GrantRead createGrant(GrantCreate grantCreate) {
        Optional<Grant> optionalGrant = getGrantByName(grantCreate.getName());
        if (optionalGrant.isPresent()) {
            throw new ConflictException("Grant already exists");
        }

        Instant now = Instant.now();
        Audit audit = new Audit();
        audit.setCreatedAt(now);
        audit.setUpdatedAt(now);

        Grant grant = grantMapper.toModel(grantCreate);
        grant.setAudit(audit);

        grant = grantRepository.save(grant);

        try {
            DiffComparer<Grant> comparer = new DiffComparer<>(null, grant);
            DiffResult diff = comparer.compare();
            DiffContext.addDiff(diff.toJson());
        } catch (Exception e) {
            // Log but don't fail if diff serialization fails
        }
        publishActivityEvent(ActivityEventType.GRANT_ADDED, grant);

        return grantMapper.toRead(grant);
    }

    private void publishActivityEvent(ActivityEventType activityEventType, Grant grant) {
        String actorId = null;
        String actorImageUrl = "";
        try {
            GrantsAuthentication auth = GrantsAuthentication.current();
            actorId = auth.getUserId();
            actorImageUrl = auth.getProfileImageUrlOrEmpty();
        } catch (IllegalStateException ignored) {
            // No authentication context (e.g., in unit tests)
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("grantId", grant.get_id().toHexString());
        metadata.put("grantName", grant.getName());
        metadata.put("grantDescription", grant.getDescription());
        metadata.put("userImageUrl", actorImageUrl);

        ActivityEvent event = ActivityEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .type(activityEventType.name())
                .actorId(actorId)
                .metadata(metadata)
                .build();
        activityFeedService.publishEvent(event);
    }
}
