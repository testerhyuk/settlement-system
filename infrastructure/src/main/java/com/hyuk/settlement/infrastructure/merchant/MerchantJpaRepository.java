package com.hyuk.settlement.infrastructure.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, String> {
    Optional<MerchantEntity> findByBusinessNumber(String businessNumber);
}