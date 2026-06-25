package com.hyuk.settlement.analytics.segment;

import com.hyuk.settlement.shared.UserSegmentEvent;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.List;

public class SegmentPerformanceEnrichmentFunction
        extends BroadcastProcessFunction<
        SegmentPerformanceEvent,
        UserSegmentEvent,
        SegmentPerformanceInput
        > {

    public static final OutputTag<UnknownSegmentEvent> UNKNOWN_SEGMENT_TAG =
            new OutputTag<UnknownSegmentEvent>("unknown-segment") {};

    public static final MapStateDescriptor<String, List<String>> USER_SEGMENT_STATE_DESCRIPTOR =
            new MapStateDescriptor<>(
                    "user-segment-broadcast-state",
                    TypeInformation.of(String.class),
                    TypeInformation.of(new TypeHint<List<String>>() {})
            );

    public static final MapStateDescriptor<String, Boolean> SEGMENT_BOOTSTRAP_STATE_DESCRIPTOR =
            new MapStateDescriptor<>(
                    "segment-bootstrap-state",
                    TypeInformation.of(String.class),
                    TypeInformation.of(Boolean.class)
            );

    private static final String BOOTSTRAP_COMPLETE_KEY = "BOOTSTRAP_COMPLETE";

    @Override
    public void processBroadcastElement(
            UserSegmentEvent event,
            Context context,
            Collector<SegmentPerformanceInput> out
    ) throws Exception {
        if (event.isBootstrapComplete()) {
            BroadcastState<String, Boolean> bootstrapState =
                    context.getBroadcastState(SEGMENT_BOOTSTRAP_STATE_DESCRIPTOR);

            bootstrapState.put(BOOTSTRAP_COMPLETE_KEY, true);
            return;
        }

        context.getBroadcastState(USER_SEGMENT_STATE_DESCRIPTOR)
                .put(event.getUserId(), event.getSegmentIds());
    }

    @Override
    public void processElement(
            SegmentPerformanceEvent event,
            ReadOnlyContext context,
            Collector<SegmentPerformanceInput> out
    ) throws Exception {
        ReadOnlyBroadcastState<String, Boolean> bootstrapState =
                context.getBroadcastState(SEGMENT_BOOTSTRAP_STATE_DESCRIPTOR);

        Boolean bootstrapComplete = bootstrapState.get(BOOTSTRAP_COMPLETE_KEY);

        if (!Boolean.TRUE.equals(bootstrapComplete)) {
            context.output(
                    UNKNOWN_SEGMENT_TAG,
                    UnknownSegmentEvent.builder()
                            .userId(event.userId())
                            .campaignId(campaignIdOf(event))
                            .eventType(event.getType())
                            .reason("SEGMENT_BOOTSTRAP_NOT_COMPLETED")
                            .build()
            );
            return;
        }

        List<String> segmentIds = context
                .getBroadcastState(USER_SEGMENT_STATE_DESCRIPTOR)
                .get(event.userId());

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