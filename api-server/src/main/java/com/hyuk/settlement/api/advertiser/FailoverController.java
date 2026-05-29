package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.infrastructure.kafka.ClusterRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ops/failover")
public class FailoverController {

    private final ClusterRouter clusterRouter;

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        String active = clusterRouter.isPrimaryActive() ? "primary" : "dr";
        return ResponseEntity.ok(active);
    }

    @PostMapping("/switch-to-dr")
    public ResponseEntity<String> switchToDr() {
        clusterRouter.switchToDr();
        return ResponseEntity.ok("Switched to DR");
    }

    @PostMapping("/switch-to-primary")
    public ResponseEntity<String> switchToPrimary() {
        clusterRouter.switchToPrimary();
        return ResponseEntity.ok("Switched to Primary");
    }
}