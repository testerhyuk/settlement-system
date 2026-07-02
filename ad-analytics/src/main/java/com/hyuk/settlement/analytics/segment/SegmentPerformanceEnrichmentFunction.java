package com.hyuk.settlement.analytics.segment;

import com.hyuk.settlement.shared.UserSegmentEvent;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.ArrayList;
import java.util.List;

public class SegmentPerformanceEnrichmentFunction
        extends KeyedProcessFunction<String, SegmentPerformanceEvent, SegmentPerformanceInput> {

    public static final OutputTag<UnknownSegmentEvent> UNKNOWN_SEGMENT_TAG =
            new OutputTag<UnknownSegmentEvent>("unknown-segment") {};

    private transient ValueState<List<String>> userSegmentState;

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<List<String>> descriptor =
                new ValueStateDescriptor<>(
                        "user-segment-keyed-state",
                        TypeInformation.of(new TypeHint<List<String>>() {})
                );
        userSegmentState = getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement(
            SegmentPerformanceEvent event,
            Context context,
            Collector<SegmentPerformanceInput> out
    ) throws Exception {
        if (event.getType() == SegmentPerformanceEventType.USER_SEGMENT) {
            updateUserSegmentState(event.getUserSegmentEvent());
            return;
        }

        List<String> segmentIds = userSegmentState.value();

        if (segmentIds == null || segmentIds.isEmpty()) {
            context.output(
                    UNKNOWN_SEGMENT_TAG,
                    UnknownSegmentEvent.builder()
                            .userId(event.userId())
                            .campaignId(campaignIdOf(event))
                            .eventType(event.getType())
                            .reason("USER_SEGMENT_STATE_NOT_FOUND")
                            .build()
            );
            return;
        }

        for (String segmentId : segmentIds) {
            emitInput(segmentId, event, out);
        }
    }

    private void updateUserSegmentState(UserSegmentEvent event) throws Exception {
        if (event.isBootstrapComplete()) {
            return;
        }

        userSegmentState.update(new ArrayList<>(event.getSegmentIds()));
    }

    private void emitInput(
            String segmentId,
            SegmentPerformanceEvent event,
            Collector<SegmentPerformanceInput> out
    ) {
        if (event.getType() == SegmentPerformanceEventType.IMPRESSION) {
            out.collect(SegmentPerformanceInput.impression(
                    segmentId,
                    event.getImpressionEvent().getCampaignId()
            ));
        }

        if (event.getType() == SegmentPerformanceEventType.CLICK) {
            out.collect(SegmentPerformanceInput.click(
                    segmentId,
                    event.getClickEvent().getCampaignId(),
                    event.getClickEvent().getCpcAmount()
            ));
        }

        if (event.getType() == SegmentPerformanceEventType.CONVERSION) {
            out.collect(SegmentPerformanceInput.conversion(
                    segmentId,
                    event.getConversionEvent().getCampaignId(),
                    event.getConversionEvent().getConversionAmount()
            ));
        }
    }

    private String campaignIdOf(SegmentPerformanceEvent event) {
        if (event.getType() == SegmentPerformanceEventType.IMPRESSION) {
            return event.getImpressionEvent().getCampaignId();
        }

        if (event.getType() == SegmentPerformanceEventType.CLICK) {
            return event.getClickEvent().getCampaignId();
        }

        if (event.getType() == SegmentPerformanceEventType.CONVERSION) {
            return event.getConversionEvent().getCampaignId();
        }

        return null;
    }
}
