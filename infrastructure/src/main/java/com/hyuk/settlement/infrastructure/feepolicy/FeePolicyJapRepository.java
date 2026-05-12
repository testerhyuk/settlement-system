package com.hyuk.settlement.infrastructure.feepolicy;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hyuk.settlement.shared.CardCompany;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FeePolicyJapRepository extends JpaRepository<FeePolicyEntity, String> {
    @Query("SELECT f FROM FeePolicyEntity f WHERE f.merchantId = :merchantId AND f.cardCompany = :cardCompany AND f.effectiveFrom <= :date AND f.effectiveTo >= :date")
    Optional<FeePolicyEntity> findActivePolicy(
            @Param("merchantId") String merchantId,
            @Param("cardCompany") CardCompany cardCompany,
            @Param("date") LocalDate date
    );
}