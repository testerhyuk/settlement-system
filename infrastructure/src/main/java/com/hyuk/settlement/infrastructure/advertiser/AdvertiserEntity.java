package com.hyuk.settlement.infrastructure.advertiser;

import com.hyuk.settlement.advertiser.Advertiser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "advertiser")
public class AdvertiserEntity {
    @Id
    private String advertiserId;
    private String brand;

    public static AdvertiserEntity from(Advertiser advertiser) {
        AdvertiserEntity advertiserEntity = new AdvertiserEntity();
        advertiserEntity.advertiserId = advertiser.getAdvertiserId();
        advertiserEntity.brand = advertiser.getBrand();

        return advertiserEntity;
    }

    public Advertiser toAdvertiser() {
        return new Advertiser(advertiserId, brand);
    }
}
