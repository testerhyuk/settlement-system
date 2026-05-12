package com.hyuk.settlement.infrastructure.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, String> {
}