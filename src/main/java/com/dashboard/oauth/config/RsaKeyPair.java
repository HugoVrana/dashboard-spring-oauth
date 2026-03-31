package com.dashboard.oauth.config;

import com.dashboard.oauth.model.entities.ServerKey;
import com.dashboard.oauth.repository.IServerKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class RsaKeyPair {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String kid;

    public RsaKeyPair(IServerKeyRepository serverKeyRepository) throws Exception {
        List<ServerKey> keys = serverKeyRepository.findAll();

        if (!keys.isEmpty()) {
            ServerKey stored = keys.getFirst();
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(stored.getPrivateKeyDer()));
            this.publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(stored.getPublicKeyDer()));
            this.kid = stored.getKid();
            log.info("Loaded RSA key pair from database (kid={})", this.kid);
        } else {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair keyPair = gen.generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.kid = UUID.randomUUID().toString();

            ServerKey serverKey = ServerKey.builder()
                    .privateKeyDer(this.privateKey.getEncoded())
                    .publicKeyDer(this.publicKey.getEncoded())
                    .kid(this.kid)
                    .build();
            serverKeyRepository.save(serverKey);
            log.info("Generated and persisted new RSA key pair (kid={})", this.kid);
        }
    }

    public RSAPrivateKey getPrivateKey() { return privateKey; }
    public RSAPublicKey getPublicKey() { return publicKey; }
    public String getKid() { return kid; }
}
