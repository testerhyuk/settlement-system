package com.hyuk.settlement.api.adserving;

import com.hyuk.settlement.shared.UserSegmentEvent;
import com.hyuk.settlement.infrastructure.adserving.UserSegmentEntity;
import com.hyuk.settlement.infrastructure.adserving.UserSegmentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SegmentBootstrapJob implements ApplicationRunner {
    private static final String BOOTSTRAP_MARKER_USER_ID = "__SEGMENT_BOOTSTRAP_COMPLETE__";

    private final SegmentBootstrapProperties properties;
    private final UserSegmentEventProducer userSegmentEventProducer;
    private final UserSegmentJpaRepository userSegmentJpaRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Segment bootstrap job is disabled");
            return;
        }

        Map<String, List<String>> userSegments = loadUserSegments();

        for (Map.Entry<String, List<String>> seed : userSegments.entrySet()) {
            userSegmentEventProducer.send(UserSegmentEvent.builder()
                    .userId(seed.getKey())
                    .segmentIds(seed.getValue())
                    .build());
        }

        userSegmentEventProducer.send(UserSegmentEvent.builder()
                .userId(BOOTSTRAP_MARKER_USER_ID)
                .segmentIds(List.of())
                .bootstrapComplete(true)
                .build());

        log.info("Segment bootstrap completed. userCount={}", userSegments.size());
    }

    private Map<String, List<String>> loadUserSegments() {
        return userSegmentJpaRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        UserSegmentEntity::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                UserSegmentEntity::getSegmentId,
                                Collectors.toCollection(ArrayList::new)
                        )
                ));
    }
}
