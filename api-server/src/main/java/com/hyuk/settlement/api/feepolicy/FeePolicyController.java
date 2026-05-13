package com.hyuk.settlement.api.feepolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fee-policy")
public class FeePolicyController {
    private final FeePolicyService feePolicyService;

    @PostMapping
    public ResponseEntity<FeePolicyResponse> register(@RequestBody RegisterFeePolicyRequest request) {
        FeePolicyResponse response = feePolicyService.registerFeePolicy(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
