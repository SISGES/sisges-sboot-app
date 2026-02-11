package com.unileste.sisges.repository;

import com.unileste.sisges.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    List<User> findAllByDeletedAtIsNull();

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByRegisterAndDeletedAtIsNull(String register);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByRegisterAndDeletedAtIsNull(String register);

    Optional<User> findByIdAndDeletedAtIsNull(Integer id);

    @Query("SELECT u.register FROM User u WHERE u.register LIKE :prefix% ORDER BY u.register DESC LIMIT 1")
    Optional<String> findLastRegisterByPrefix(@Param("prefix") String prefix);
}
