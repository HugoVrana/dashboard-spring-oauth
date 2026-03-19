package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Grant Service")
@ExtendWith(MockitoExtension.class)
class GrantServiceTest {

    @Mock
    private IGrantRepository grantRepository;

    @Mock
    private IGrantMapper grantMapper;

    @Mock
    private IActivityFeedService activityFeedService;

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
    @DisplayName("Get existing Grant by Name")
    void getGrantByName_shouldReturnGrant_whenGrantExists() {
        when(grantRepository.findByName(testGrantName)).thenReturn(Optional.of(testGrant));

        Optional<Grant> result = grantService.getGrantByName(testGrantName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(testGrantName);
        verify(grantRepository).findByName(testGrantName);
    }

    @Test
    @DisplayName("Get nonexistent Grant by Name")
    void getGrantByName_shouldReturnEmpty_whenGrantNotFound() {
        when(grantRepository.findByName(testGrantName)).thenReturn(Optional.empty());

        Optional<Grant> result = grantService.getGrantByName(testGrantName);

        assertThat(result).isEmpty();
        verify(grantRepository).findByName(testGrantName);
    }

    @Test
    @DisplayName("Get existing Grant by Id")
    void getGrantById_shouldReturnGrant_whenGrantExists() {
        when(grantRepository.findById(testGrantId)).thenReturn(Optional.of(testGrant));

        Optional<Grant> result = grantService.getGrantById(testGrantId);

        assertThat(result).isPresent();
        assertThat(result.get().get_id()).isEqualTo(testGrantId);
        verify(grantRepository).findById(testGrantId);
    }

    @Test
    @DisplayName("Get nonexistent Grant by Id")
    void getGrantById_shouldReturnEmpty_whenGrantNotFound() {
        when(grantRepository.findById(testGrantId)).thenReturn(Optional.empty());

        Optional<Grant> result = grantService.getGrantById(testGrantId);

        assertThat(result).isEmpty();
        verify(grantRepository).findById(testGrantId);
    }

    @Test
    @DisplayName("Create Grant")
    void createGrant_shouldReturnCreatedGrant() {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription(testGrant.getDescription());

        GrantRead expectedRead = new GrantRead();
        expectedRead.setName(testGrantName);
        expectedRead.setDescription(testGrant.getDescription());

        when(grantRepository.findByName(testGrantName)).thenReturn(Optional.empty());
        when(grantMapper.toModel(any(GrantCreate.class))).thenReturn(testGrant);
        when(grantRepository.save(any(Grant.class))).thenReturn(testGrant);
        when(grantMapper.toRead(any(Grant.class))).thenReturn(expectedRead);

        GrantRead result = grantService.createGrant(grantCreate);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testGrantName);
        verify(grantRepository).save(any(Grant.class));
    }

    @Test
    @DisplayName("Get all grants returns list")
    void getGrants_shouldReturnAllGrants() {
        GrantRead expectedRead = new GrantRead();
        expectedRead.setName(testGrantName);
        expectedRead.setDescription(testGrant.getDescription());

        when(grantRepository.findAll()).thenReturn(List.of(testGrant));
        when(grantMapper.toRead(testGrant)).thenReturn(expectedRead);

        List<GrantRead> result = grantService.getGrants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(testGrantName);
    }

    @Test
    @DisplayName("Get grant read by id returns DTO when grant exists")
    void getGrantReadById_shouldReturnGrantRead_whenExists() {
        GrantRead expectedRead = new GrantRead();
        expectedRead.setName(testGrantName);

        when(grantRepository.findById(testGrantId)).thenReturn(Optional.of(testGrant));
        when(grantMapper.toRead(testGrant)).thenReturn(expectedRead);

        GrantRead result = grantService.getGrantReadById(testGrantId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testGrantName);
    }

    @Test
    @DisplayName("Get grant read by id throws ResourceNotFoundException when not found")
    void getGrantReadById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(grantRepository.findById(testGrantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> grantService.getGrantReadById(testGrantId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Delete grant removes entity when it exists")
    void deleteGrant_shouldDeleteGrant_whenExists() {
        when(grantRepository.findById(testGrantId)).thenReturn(Optional.of(testGrant));

        grantService.deleteGrant(testGrantId);

        verify(grantRepository).delete(testGrant);
    }

    @Test
    @DisplayName("Delete grant throws ResourceNotFoundException when not found")
    void deleteGrant_shouldThrowResourceNotFoundException_whenNotFound() {
        when(grantRepository.findById(testGrantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> grantService.deleteGrant(testGrantId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
