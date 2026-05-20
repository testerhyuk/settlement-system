package com.hyuk.settlement.api.merchant;

import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MerchantService {
    private final MerchantRepository merchantRepository;

    @Transactional
    public void changeBankAccount(ChangeBankAccountRequest request) {
        Merchant merchant = merchantRepository.findByBusinessNumber(request.getBusinessNumber())
                .orElseThrow(() -> new IllegalStateException("해당 가맹점을 찾을 수 없습니다"));

        if (!merchant.getBankAccount().getAccountHolder().equals(request.getBankAccount().getAccountHolder())) {
            throw new IllegalStateException("가맹점 정보가 일치하지 않습니다");
        }

        merchant.changeBankAccount(request.getBankAccount());

        merchantRepository.save(merchant);
    }

    @Transactional
    public MerchantResponse saveMerchant(RegisterMerchantRequest registerMerchantRequest) {
        merchantRepository.findByBusinessNumber(registerMerchantRequest.getBusinessNumber())
                .ifPresent(m -> { throw new IllegalStateException("이미 등록된 가맹점입니다"); });

        Merchant merchant = Merchant.create(
                registerMerchantRequest.getBusinessNumber(),
                registerMerchantRequest.getName(),
                registerMerchantRequest.getBankAccount(),
                registerMerchantRequest.getSettlementCycle()
                );

        merchantRepository.save(merchant);

        return convertToMerchantResponse(merchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponse findById(String merchantId) {
        return merchantRepository.findById(merchantId)
                .map(this::convertToMerchantResponse)
                .orElseThrow(() -> new IllegalStateException("가맹점을 찾을 수 없습니다"));
    }

    private MerchantResponse convertToMerchantResponse(Merchant merchant) {
        return MerchantResponse.builder()
                .merchantId(merchant.getMerchantId())
                .businessNumber(merchant.getBusinessNumber())
                .name(merchant.getName())
                .bankAccount(merchant.getBankAccount())
                .settlementCycle(merchant.getSettlementCycle())
                .build();
    }
}
