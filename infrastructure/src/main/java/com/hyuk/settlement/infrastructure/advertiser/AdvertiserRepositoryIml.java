package com.hyuk.settlement.infrastructure.advertiser;

import com.hyuk.settlement.advertiser.Advertiser;
import com.hyuk.settlement.advertiser.AdvertiserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdvertiserRepositoryIml implements AdvertiserRepository {
    private final AdvertiserJpaRepository advertiserJpaRepository;

    @Override
    public Optional<Advertiser> findById(String advertiserId) {
        return advertiserJpaRepository.findById(advertiserId).map(AdvertiserEntity::toAdvertiser);
    }

    @Override
    public Advertiser save(Advertiser advertiser) {
        AdvertiserEntity entity = AdvertiserEntity.from(advertiser);
        advertiserJpaRepository.save(entity);
        return entity.toAdvertiser();
    }
}
