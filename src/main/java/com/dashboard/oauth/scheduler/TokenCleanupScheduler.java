package com.dashboard.oauth.scheduler;

import com.dashboard.oauth.model.entities.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class TokenCleanupScheduler {

    @Autowired
    private MongoTemplate mongoTemplate;

    // Run daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        // Clear expired/used email verification tokens
        Query query = new Query()
                .addCriteria(Criteria.where("emailVerificationToken.expiryDate").lt(cutoffDate)
                        .orOperator(Criteria.where("emailVerificationToken.used").is(true)));

        Update update = new Update().unset("emailVerificationToken");
        mongoTemplate.updateMulti(query, update, User.class);

        // Clear expired/used password reset tokens
        query = new Query()
                .addCriteria(Criteria.where("passwordResetToken.expiryDate").lt(cutoffDate)
                        .orOperator(Criteria.where("passwordResetToken.used").is(true)));

        update = new Update().unset("passwordResetToken");
        mongoTemplate.updateMulti(query, update, User.class);
    }
}
