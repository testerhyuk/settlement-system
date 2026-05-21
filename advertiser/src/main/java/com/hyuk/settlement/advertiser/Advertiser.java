package com.hyuk.settlement.advertiser;

import com.hyuk.settlement.shared.Money;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Advertiser {
    private String advertiserId;
    private String brand;

    public Advertiser(String advertiserId, String brand) {
        if (advertiserId == null || advertiserId.isBlank()) {
            throw new IllegalArgumentException("Advertiser ID는 필수입니다");
        }

        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("브랜드 혹은 회사명은 필수입니다");
        }

        this.advertiserId = advertiserId;
        this.brand = brand;
    }

    public static Advertiser create(String brand) {
        return new Advertiser(
                "advertiser-" + UUID.randomUUID().toString(),
                brand
        );
    }
}
