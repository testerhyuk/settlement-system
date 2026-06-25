package com.hyuk.settlement.api.adserving;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ad-serving")
public class AdServingController {
    private final AdServingService adServingService;

    @GetMapping("/ads")
    public ResponseEntity<List<AdServingResponse>> getServingCandidates(@RequestParam("userId") String userId) {
        return ResponseEntity.ok(adServingService.adServingCandidates(userId));
    }

    @PostMapping("/impressions")
    public ResponseEntity<Void> recordImpressions(@RequestBody List<AdImpressionRequest> requestList) {
        adServingService.recordImpressions(requestList);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/clicks")
    public ResponseEntity<Void> recordClicks(@RequestBody List<AdClickRequest> requestList) {
        adServingService.recordClicks(requestList);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversions")
    public ResponseEntity<Void> recordConversions(@RequestBody List<AdConversionRequest> requestList) {
        adServingService.recordConversions(requestList);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/user-segments")
    public ResponseEntity<Void> recordUserSegments(@RequestBody List<UserSegmentRequest> requestList) {
        adServingService.recordUserSegments(requestList);

        return ResponseEntity.ok().build();
    }
}
