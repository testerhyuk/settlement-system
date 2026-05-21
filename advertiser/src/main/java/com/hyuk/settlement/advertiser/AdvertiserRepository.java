package com.hyuk.settlement.advertiser;

import java.util.Optional;

public interface AdvertiserRepository {
    Optional<Advertiser> findById(String advertiserId);
    Advertiser save(Advertiser advertiser);
}
