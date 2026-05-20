package com.hyuk.settlement.api.merchant;

import com.hyuk.settlement.shared.BankAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class MerchantController {
    private final MerchantService merchantService;

    @PostMapping
    public ResponseEntity<MerchantResponse> saveMerchant(@RequestBody RegisterMerchantRequest request) {
        MerchantResponse response = merchantService.saveMerchant(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable String merchantId) {
        MerchantResponse response = merchantService.findById(merchantId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/account")
    public ResponseEntity<Void> changeBankAccount(@RequestBody ChangeBankAccountRequest request) {
        merchantService.changeBankAccount(request);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}