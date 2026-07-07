package com.bernardo.geradortimes.support;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubJersey;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.club.repository.ClubJerseyRepository;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.club.repository.ClubRepository;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.shared.enums.UserRole;
import com.bernardo.geradortimes.shared.value_object.Email;
import com.bernardo.geradortimes.shared.value_object.HexColor;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.shared.value_object.PasswordHash;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.user.model.VerificationToken;
import com.bernardo.geradortimes.user.repository.UserRepository;
import com.bernardo.geradortimes.user.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Base class for integration tests.
 * <p>
 * Starts the full Spring context with PostgreSQL (Testcontainers) and Flyway migrations.
 * Mocks RabbitMQ and JavaMailSender to avoid external connections.
 * Provides helpers to create test data and generate JWT tokens.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("geradortimes_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        if (!POSTGRESQL.isRunning()) {
            POSTGRESQL.start();
        }
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRESQL::getDriverClassName);
    }

    // ── JWT constants (must match test profile) ─────────────────────────────
    protected static final String JWT_SECRET  = "dev-test-secret-key-must-be-at-least-32-chars-long";
    protected static final String JWT_ISSUER  = "geradortimes-test";
    protected static final long   JWT_TTL_SEC = 900L; // 15 min

    // ── Mocked external dependencies ────────────────────────────────────────
    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    @MockitoBean
    protected JavaMailSender javaMailSender;

    // ── Spring MVC / JSON ────────────────────────────────────────────────────
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── Repositories ─────────────────────────────────────────────────────────
    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ClubRepository clubRepository;

    @Autowired
    protected ClubMemberRepository clubMemberRepository;

    @Autowired
    protected ClubJerseyRepository clubJerseyRepository;

    @Autowired
    protected VerificationTokenRepository verificationTokenRepository;

    // ── Cleanup ───────────────────────────────────────────────────────────────
    @BeforeEach
    void cleanDatabase() {
        clubMemberRepository.deleteAll();
        clubJerseyRepository.deleteAll();
        clubRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── JWT helpers ───────────────────────────────────────────────────────────

    /**
     * Generates a valid Bearer JWT for the given user.
     */
    protected String bearerToken(User user) {
        return "Bearer " + generateJwt(user.getId(), user.getLogin().getValue(), user.getRole());
    }

    /**
     * Generates a valid Bearer JWT for the given parameters.
     */
    protected String bearerToken(UUID userId, String login, UserRole role) {
        return "Bearer " + generateJwt(userId, login, role);
    }

    private String generateJwt(UUID userId, String login, UserRole role) {
        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(JWT_ISSUER)
                .withSubject(userId.toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(JWT_TTL_SEC)))
                .withClaim("login", login)
                .withClaim("nickname", login)
                .withClaim("role", role.name())
                .sign(algorithm);
    }

    // ── Data-builder helpers ──────────────────────────────────────────────────

    /**
     * Persists an ACTIVE user with the given login/nickname.
     * Password is stored as a plain-text hash placeholder (not used for auth in tests).
     */
    protected User createActiveUser(String login, String nickname) {
        User user = User.create(
                "Test User",
                Nickname.of(nickname),
                Email.of(login),
                PasswordHash.fromEncoded("$argon2id$v=19$m=65536,t=3,p=4$placeholder$placeholder")
        );
        user.activateUser();
        return userRepository.save(user);
    }

    /**
     * Persists an ACTIVE admin user.
     */
    protected User createAdminUser(String login, String nickname) {
        User user = createActiveUser(login, nickname);
        // Promote to ADMIN via reflection (role field is private, no setter by design)
        try {
            Field roleField = User.class.getDeclaredField("role");
            roleField.setAccessible(true);
            roleField.set(user, UserRole.ADMIN);
        } catch (Exception e) {
            throw new RuntimeException("Could not set admin role via reflection", e);
        }
        return userRepository.save(user);
    }

    /**
     * Creates and persists a Club.
     */
    protected Club createClub(String name, String nickname) {
        Club club = Club.create(name, Nickname.of(nickname));
        return clubRepository.save(club);
    }

    /**
     * Creates and persists a ClubMember linking a user to a club with the given role.
     */
    protected ClubMember createClubMember(UUID userId, UUID clubId, ClubRole role) {
        ClubMember member = ClubMember.create(userId, clubId, "Member Name", 5, 0, 0, null, role);
        return clubMemberRepository.save(member);
    }

    /**
     * Creates and persists a ClubJersey for the given club.
     */
    protected ClubJersey createJersey(UUID clubId, String name, String hexColor) {
        ClubJersey jersey = ClubJersey.create(HexColor.of(hexColor), name, false, clubId);
        return clubJerseyRepository.save(jersey);
    }

    /**
     * Generates a registration JWT for the given email, matching the format
     * produced by {@link com.bernardo.geradortimes.auth.security.JwtService#issueRegistrationToken}.
     */
    protected String createRegistrationJwt(String email) {
        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(JWT_ISSUER)
                .withSubject(email)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(1800)))
                .withClaim("email", email)
                .withClaim("purpose", "registration")
                .sign(algorithm);
    }

    /**
     * Generates a 6-digit verification token, hashes it (SHA-256), persists a
     * {@link VerificationToken} of type {@link TokenType#EMAIL_VERIFICATION}
     * with a 15-minute expiry for the given email, and returns the plain token.
     */
    protected String createVerificationToken(String email) {
        return createToken(email, TokenType.EMAIL_VERIFICATION);
    }

    /**
     * Generates a 6-digit password reset token, hashes it (SHA-256), persists a
     * {@link VerificationToken} of type {@link TokenType#PASSWORD_RESET}
     * with a 15-minute expiry for the given user, and returns the plain token.
     */
    protected String createPasswordResetToken(User user) {
        return createToken(user.getLogin().getValue(), TokenType.PASSWORD_RESET);
    }

    private String createToken(String email, TokenType type) {
        int raw = 100000 + new java.security.SecureRandom().nextInt(900000);
        String token = String.valueOf(raw);
        String hash = sha256(token);
        VerificationToken vt = VerificationToken.create(
                hash,
                type,
                Instant.now().plus(15, ChronoUnit.MINUTES),
                email
        );
        verificationTokenRepository.save(vt);
        return token;
    }

    protected static String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialises an object to JSON string.
     */
    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
