package com.hyuk.settlement.api.adserving;

public interface AdTrackingRequest {
    String getImpressionId();
    String getCampaignId();
    String getAdvertiserId();
    String getUserId();
}
