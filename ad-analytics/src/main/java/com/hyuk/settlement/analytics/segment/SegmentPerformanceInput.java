package com.hyuk.settlement.analytics.segment;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class SegmentPerformanceInput {
    private SegmentPerformanceInputType type;

    private String segmentId;
    private String campaignId;

    private Long impressionCount;
    private Long clickCount;
    private Long conversionCount;

    private BigDecimal totalClickCost;
    private BigDecimal totalConversionAmount;

    public static SegmentPerformanceInput impression(String segmentId, String campaignId) {
        SegmentPerformanceInput input = new SegmentPerformanceInput();
        input.type = SegmentPerformanceInputType.IMPRESSION;
        input.segmentId = segmentId;
        input.campaignId = campaignId;
        input.impressionCount = 1L;

        return input;
    }

    public static SegmentPerformanceInput click(
            String segmentId,
            String campaignId,
            BigDecimal totalClickCost
    ) {
        SegmentPerformanceInput input = new SegmentPerformanceInput();
        input.type = SegmentPerformanceInputType.CLICK;
        input.segmentId = segmentId;
        input.campaignId = campaignId;
        input.clickCount = 1L;
        input.totalClickCost = totalClickCost;

        return input;
    }

    public static SegmentPerformanceInput conversion(
            String segmentId,
            String campaignId,
            BigDecimal totalConversionAmount
    ) {
        SegmentPerformanceInput input = new SegmentPerformanceInput();
        input.type = SegmentPerformanceInputType.CONVERSION;
        input.segmentId = segmentId;
        input.campaignId = campaignId;
        input.conversionCount = 1L;
        input.totalConversionAmount = totalConversionAmount;

        return input;
    }
}