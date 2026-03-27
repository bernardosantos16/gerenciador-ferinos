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
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }
}

