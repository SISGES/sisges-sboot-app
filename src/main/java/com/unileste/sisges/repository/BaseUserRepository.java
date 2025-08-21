package com.unileste.sisges.repository;

import com.unileste.sisges.model.BaseUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BaseUserRepository extends JpaRepository<BaseUser, Integer> {
    Optional<BaseUser> findByEmail(String email);
}