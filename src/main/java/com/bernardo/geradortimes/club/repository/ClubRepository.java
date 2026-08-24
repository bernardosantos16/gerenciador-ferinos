package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubRepository extends JpaRepository<Club, UUID> {
    Optional<Club> findByNicknameValue(String nickname);

    boolean existsByNicknameValue(String nickname);

    boolean existsByNicknameValueAndIdNot(String nickname, UUID id);

    @Query(
            value = """
                    SELECT * FROM clubs c
                    WHERE c.status = 'ACTIVE'
                      AND (c.name ILIKE :pattern OR c.nickname ILIKE :pattern)
                    ORDER BY c.name
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Club> searchActive(@Param("pattern") String pattern, @Param("limit") int limit);
}
