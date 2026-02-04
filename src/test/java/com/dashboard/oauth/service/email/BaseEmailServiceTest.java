package com.dashboard.oauth.service.email;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.entities.VerificationToken;
import com.dashboard.oauth.repository.IEmailSendAttemptRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.EmailService;
import com.dashboard.oauth.service.interfaces.IEmailSenderService;
import com.dashboard.oauth.service.interfaces.IEmailTemplateService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@Epic("Authentication")
@Feature("Email Service")
@Tag("service-email")
@ExtendWith(MockitoExtension.class)
public abstract class BaseEmailServiceTest {
    @Mock
    protected IUserRepository userRepository;

    @Mock
    protected IEmailSenderService emailSenderService;

    @Mock
    protected IEmailSendAttemptRepository emailSendAttemptRepository;

    @Mock
    protected IEmailTemplateService emailTemplateService;

    protected EmailService emailService;

    protected final Faker faker = new Faker();

    protected User testUser;
    protected VerificationToken testToken;
    protected String testEmail;
    protected ObjectId testUserId;

    protected VerificationToken createTestToken() {
        VerificationToken token = new VerificationToken();
        token.set_id(ObjectId.get());
        token.setExpiryDate(Instant.now().plusSeconds(86400));
        token.setCreatedAt(Instant.now());
        token.setUsed(false);
        return token;
    }

    protected User createTestUser() {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail(testEmail);
        user.setPassword(faker.internet().password());
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }

    @BeforeEach
    void setUp() {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.setBaseUrl("http://localhost:3000");
        emailProperties.setVerificationTokenExpirationMs(86400000L); // 24 hours
        emailProperties.setPasswordResetTokenExpirationMs(3600000L); // 1 hour
        emailService = new EmailService(userRepository, emailSenderService, emailSendAttemptRepository, emailTemplateService, emailProperties);

        testUserId = new ObjectId();
        testEmail = faker.internet().emailAddress();
        testToken = createTestToken();
        testUser = createTestUser();

        // Default template mocks - return simple HTML content (lenient because not all tests use both)
        lenient().when(emailTemplateService.renderVerificationEmail(anyString(), anyLong()))
                .thenReturn("<html>Verify email</html>");
        lenient().when(emailTemplateService.renderPasswordResetEmail(anyString(), anyLong()))
                .thenReturn("<html>Reset password</html>");
    }
}
