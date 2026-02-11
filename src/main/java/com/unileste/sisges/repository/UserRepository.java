package com.unileste.sisges.repository;

import com.unileste.sisges.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByRegisterAndDeletedAtIsNull(String register);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByRegisterAndDeletedAtIsNull(String register);
}
