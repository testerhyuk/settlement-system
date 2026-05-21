package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.advertiser.AdCampaignStatus;
import com.hyuk.settlement.advertiser.AdvertiserRepository;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertiserServiceTest {
    @Mock
    private AdvertiserRepository advertiserRepository;

    @Mock
    private AdCampaignRepository adCampaignRepository;

    @InjectMocks
    private AdvertiserService advertiserService;

    private AdCampaign adCampaign;
    private AdCampaign exhausted;
    private AdCampaign paused;
    private AdCampaign ended;

    @BeforeEach
    void setUp() {
        adCampaign = new AdCampaign(
                "campaign-1",
                "advertiser-1",
                new Money(BigDecimal.valueOf(10000), Currency.KRW),
                new Money(BigDecimal.valueOf(200000), Currency.KRW),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                AdCampaignStatus.ACTIVE
        );

        exhausted = new AdCampaign(
                "campaign-2",
                "advertiser-2",
                new Money(BigDecimal.valueOf(10000), Currency.KRW),
                new Money(BigDecimal.valueOf(10000), Currency.KRW),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                AdCampaignStatus.ACTIVE
        );

        paused = new AdCampaign(
                "campaign-3",
                "advertiser-3",
                new Money(BigDecimal.valueOf(10000), Currency.KRW),
                new Money(BigDecimal.valueOf(100000), Currency.KRW),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                AdCampaignStatus.PAUSED
        );

        ended = new AdCampaign(
                "campaign-4",
                "advertiser-4",
                new Money(BigDecimal.valueOf(10000), Currency.KRW),
                new Money(BigDecimal.valueOf(100000), Currency.KRW),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 3),
                AdCampaignStatus.ENDED
        );
    }

    @Test
    void 광고_클릭시_예산_차감() {
        when(adCampaignRepository.findActiveCampaign(any(), any())).thenReturn(Optional.ofNullable(adCampaign));

        advertiserService.processAdClick("campaign-1");

        ArgumentCaptor<AdCampaign> captor = ArgumentCaptor.forClass(AdCampaign.class);

        verify(adCampaignRepository, times(1)).findActiveCampaign(any(), any());
        verify(adCampaignRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getBudget().getAmount()).isEqualTo(BigDecimal.valueOf(190000));
    }

    @Test
    void 예산_소진시_BUDGET_EXHAUSTED로_상태_변경() {
        when(adCampaignRepository.findActiveCampaign(any(), any())).thenReturn(Optional.ofNullable(exhausted));

        advertiserService.processAdClick("campaign-2");

        ArgumentCaptor<AdCampaign> captor = ArgumentCaptor.forClass(AdCampaign.class);

        verify(adCampaignRepository, times(1)).findActiveCampaign(any(), any());
        verify(adCampaignRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getBudget().getAmount()).isEqualTo(BigDecimal.valueOf(0));
        assertThat(captor.getValue().getStatus()).isEqualByComparingTo(AdCampaignStatus.BUDGET_EXHAUSTED);
    }

    @Test
    void 데이터가_없는_상태라면_예외_발생() {
        when(adCampaignRepository.findActiveCampaign(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> advertiserService.processAdClick("campaign-n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캠페인이 존재하지 않습니다");
    }

    @Test
    void PAUSED_상태라면_예외_발생() {
        when(adCampaignRepository.findActiveCampaign(any(), any())).thenReturn(Optional.empty());
        when(adCampaignRepository.findById(any())).thenReturn(Optional.of(paused));

        assertThatThrownBy(() -> advertiserService.processAdClick("campaign-3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("캠페인 상태가 PAUSED입니다");
    }

    @Test
    void ENDED_상태라면_예외_발생() {
        when(adCampaignRepository.findActiveCampaign(any(), any())).thenReturn(Optional.empty());
        when(adCampaignRepository.findById(any())).thenReturn(Optional.of(ended));

        assertThatThrownBy(() -> advertiserService.processAdClick("campaign-4"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("캠페인 상태가 ENDED입니다");
    }
}