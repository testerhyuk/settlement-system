package com.hyuk.settlement.api.adserving;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdServingController {
    private final AdServingService adServingService;

    @GetMapping("/api/ad-serving/ads")
    public ResponseEntity<List<AdServingResponse>> getServingCandidates() {
        return ResponseEntity.ok(adServingService.adServingCandidates());
    }
}
