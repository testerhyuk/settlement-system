package com.hyuk.settlement.api.dlq;

import com.hyuk.settlement.infrastructure.dlq.DlqMessageService;
import com.hyuk.settlement.infrastructure.dlq.DlqResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dlq")
public class DlqMessageController {
    private final DlqMessageService dlqMessageService;

    @GetMapping
    public ResponseEntity<List<DlqResponseDto>> getDlqMessage() {
        List<DlqResponseDto> responseList = dlqMessageService.getDlqMessage();

        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/{dlqMessageId}/discard")
    public ResponseEntity<Void> discardMessage(@PathVariable("dlqMessageId") String dlqMessageId) {
        dlqMessageService.discardDlqMessage(dlqMessageId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{dlqMessageId}/replay")
    public ResponseEntity<Void> replayMessage(@PathVariable("dlqMessageId") String dlqMessageId) {
        dlqMessageService.replay(dlqMessageId);

        return ResponseEntity.ok().build();
    }
}
