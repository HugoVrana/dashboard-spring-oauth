package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.repository.IGrantRepository;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrantServiceTest {

    @Mock
    private IGrantRepository grantRepository;

    @InjectMocks
    private GrantService grantService;

    private final Faker faker = new Faker();

    private ObjectId testGrantId;
    private String testGrantName;
    private Grant testGrant;

    @BeforeEach
    void setUp() {
        testGrantId = new ObjectId();
        testGrantName = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();
        testGrant = createTestGrant();
    }

    private Grant createTestGrant() {
        Grant grant = new Grant();
        grant.set_id(testGrantId);
        grant.setName(testGrantName);
        grant.setDescription(faker.lorem().sentence());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        grant.setAudit(audit);

        return grant;
    }

    @Test
    void getGrantByName_shouldReturnGrant_whenGrantExists() {
        when(grantRepository.findByName(testGrantName)).thenReturn(Optional.of(testGrant));

        Optional<Grant> result = grantService.getGrantByName(testGrantName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(testGrantName);
        verify(grantRepository).findByName(testGrantName);
    }

    @Test
    void getGrantByName_shouldReturnEmpty_whenGrantNotFound() {
        when(grantRepository.findByName(testGrantName)).thenReturn(Optional.empty());

        Optional<Grant> result = grantService.getGrantByName(testGrantName);

        assertThat(result).isEmpty();
        verify(grantRepository).findByName(testGrantName);
    }

    @Test
    void getGrantById_shouldReturnGrant_whenGrantExists() {
        when(grantRepository.findById(testGrantId)).thenReturn(Optional.of(testGrant));

        Optional<Grant> result = grantService.getGrantById(testGrantId);

        assertThat(result).isPresent();
        assertThat(result.get().get_id()).isEqualTo(testGrantId);
        verify(grantRepository).findById(testGrantId);
    }

    @Test
    void getGrantById_shouldReturnEmpty_whenGrantNotFound() {
        when(grantRepository.findById(testGrantId)).thenReturn(Optional.empty());

        Optional<Grant> result = grantService.getGrantById(testGrantId);

        assertThat(result).isEmpty();
        verify(grantRepository).findById(testGrantId);
    }

    @Test
    void createGrant_shouldReturnCreatedGrant() {
        when(grantRepository.save(any(Grant.class))).thenReturn(testGrant);

        Grant result = grantService.createGrant(testGrant);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testGrantName);
        verify(grantRepository).save(testGrant);
    }
}
