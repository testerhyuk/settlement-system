package com.hyuk.settlement.infrastructure.merchant;

import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MerchantRepositoryImpl implements MerchantRepository {
    private final MerchantJpaRepository merchantJpaRepository;

    @Override
    public Merchant save(Merchant merchant) {
        MerchantEntity merchantEntity = MerchantEntity.from(merchant);
        merchantJpaRepository.save(merchantEntity);
        return merchantEntity.toDomain();
    }

    @Override
    public Optional<Merchant> findById(String id) {
        return merchantJpaRepository.findById(id).map(MerchantEntity::toDomain);
    }
}
