package com.sushishop.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sushishop.shared.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findAllByUserRoleOrderByNameAsc(UserRole userRole);

    boolean existsByEmail(String email);
}
