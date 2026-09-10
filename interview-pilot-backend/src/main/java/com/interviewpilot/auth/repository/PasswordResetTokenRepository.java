package com.interviewpilot.auth.repository;

import com.interviewpilot.auth.entity.PasswordResetToken;
import com.interviewpilot.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    boolean existsByUserAndCreatedAtAfter(User user, LocalDateTime createdAt);

    @Modifying(flushAutomatically = true)
    @Query("update PasswordResetToken token set token.used = true "
            + "where token.user = :user and token.used = false")
    void invalidateActiveTokensForUser(@Param("user") User user);
}
