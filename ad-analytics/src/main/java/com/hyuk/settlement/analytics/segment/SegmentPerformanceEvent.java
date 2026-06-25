package com.hyuk.settlement.analytics.segment;

import com.hyuk.settlement.shared.AdClickEvent;
import com.hyuk.settlement.shared.AdConversionEvent;
import com.hyuk.settlement.shared.AdImpressionEvent;
import com.hyuk.settlement.shared.UserSegmentEvent;
import lombok.Getter;

@Getter
public class SegmentPerformanceEvent {
    private SegmentPerformanceEventType type;

    private AdImpressionEvent impressionEvent;
    private AdClickEvent clickEvent;
    private AdConversionEvent conversionEvent;
    private UserSegmentEvent userSegmentEvent;

    public static SegmentPerformanceEvent impression(AdImpressionEvent event) {
        SegmentPerformanceEvent wrapper = new SegmentPerformanceEvent();
        wrapper.type = SegmentPerformanceEventType.IMPRESSION;
        wrapper.impressionEvent = event;
        return wrapper;
    }

    public static SegmentPerformanceEvent click(AdClickEvent event) {
        SegmentPerformanceEvent wrapper = new SegmentPerformanceEvent();
        wrapper.type = SegmentPerformanceEventType.CLICK;
        wrapper.clickEvent = event;
        return wrapper;
    }

    public static SegmentPerformanceEvent conversion(AdConversionEvent event) {
        SegmentPerformanceEvent wrapper = new SegmentPerformanceEvent();
        wrapper.type = SegmentPerformanceEventType.CONVERSION;
        wrapper.conversionEvent = event;
        return wrapper;
    }

    public static SegmentPerformanceEvent userSegment(UserSegmentEvent event) {
        SegmentPerformanceEvent wrapper = new SegmentPerformanceEvent();
        wrapper.type = SegmentPerformanceEventType.USER_SEGMENT;
        wrapper.userSegmentEvent = event;
        return wrapper;
    }

    public String userId() {
        if (type == SegmentPerformanceEventType.IMPRESSION) {
            return impressionEvent.getUserId();
        }

        if (type == SegmentPerformanceEventType.CLICK) {
            return clickEvent.getUserId();
        }

        if (type == SegmentPerformanceEventType.CONVERSION) {
            return conversionEvent.getUserId();
        }

        if (type == SegmentPerformanceEventType.USER_SEGMENT) {
            return userSegmentEvent.getUserId();
        }

        return null;
    }
}