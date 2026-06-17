package com.litsite.lit.repository;

import com.litsite.lit.models.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<MyUser, Long> {
    Optional<MyUser> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Optional<MyUser> findByEmail(String email);
        @Query("SELECT COUNT(u) FROM MyUser u JOIN u.roles r WHERE r.name = :roleName")
        long countByRolesName(@Param("roleName") String roleName);
}

