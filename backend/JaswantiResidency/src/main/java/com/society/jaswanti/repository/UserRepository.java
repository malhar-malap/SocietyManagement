package com.society.jaswanti.repository;

import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    long countByRoleAndIsActiveTrueAndStatus(Role role, UserStatus status);
}
