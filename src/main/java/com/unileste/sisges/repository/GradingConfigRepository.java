package com.unileste.sisges.repository;

import com.unileste.sisges.model.GradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradingConfigRepository extends JpaRepository<GradingConfig, Integer> {

    Optional<GradingConfig> findFirstByOrderByIdAsc();
}
