package com.bernardo.geradortimes.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bernardo.geradortimes.auth.config.JwtProperties;
import com.bernardo.geradortimes.user.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties props;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(JwtProperties props) {
        this.props = props;
        if (props.secret() == null || props.secret().isBlank()) {
            throw new IllegalStateException("Missing JWT secret. Configure auth.jwt.secret");
        }
        if (props.secret().length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 chars");
        }

        String issuer = (props.issuer() == null || props.issuer().isBlank()) ? "geradortimes" : props.issuer();
        this.algorithm = Algorithm.HMAC256(props.secret());
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.accessTokenTtl());
        String issuer = (props.issuer() == null || props.issuer().isBlank()) ? "geradortimes" : props.issuer();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getId().toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .withClaim("login", user.getLogin().getValue())
                .withClaim("nickname", user.getNickname().getValue())
                .withClaim("role", user.getRole().name())
                .withClaim("status", user.getStatus().name())
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }

    public String issueRegistrationToken(String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.registrationTokenTtl());
        String issuer = (props.issuer() == null || props.issuer().isBlank()) ? "geradortimes" : props.issuer();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(email)
                .withClaim("email", email)
                .withClaim("purpose", "registration")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public String verifyRegistrationToken(String token) throws JWTVerificationException {
        DecodedJWT jwt = verifier.verify(token);
        String purpose = jwt.getClaim("purpose").asString();
        if (!"registration".equals(purpose)) {
            throw new JWTVerificationException("Invalid token purpose");
        }
        String email = jwt.getClaim("email").asString();
        if (email == null || email.isBlank()) {
            throw new JWTVerificationException("Missing email claim in registration token");
        }
        return email;
    }
}
