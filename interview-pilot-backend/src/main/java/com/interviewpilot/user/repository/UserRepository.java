package com.interviewpilot.user.repository;

import com.interviewpilot.common.enums.Role;
import com.interviewpilot.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    List<User> findTop10ByOrderByCreatedAtDesc();
}
