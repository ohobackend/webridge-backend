package com.webridge.terms.repository;

import com.webridge.terms.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermsRepository extends JpaRepository<Terms, Long> {
    Optional<Terms> findByType(String type);
    Optional<Terms> findByTypeAndIsActiveTrue(String type);
}
