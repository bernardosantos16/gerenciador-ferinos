package com.bernardo.geradortimes.user.repository;

import com.bernardo.geradortimes.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByLogin_Value(String value);

    boolean existsByLogin_Value(String value);

    boolean existsByNickname_Value(String value);

    Optional<User> findByEmailVerificationToken(String token);
}
