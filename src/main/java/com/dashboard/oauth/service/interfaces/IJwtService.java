package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.auth.Grant;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.function.Function;

public interface IJwtService {
     String generateToken(UserInfo userDetails, List<Grant> allowedGrants);

     String generateIdToken(UserInfo userInfo, String clientId, String nonce);

     String extractUsername(String token);

     <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

     Boolean validateToken(String token, UserDetails userDetails);
}
