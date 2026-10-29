package com.sagares.saga_res_api.common.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeListener {
    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChange(RealtimeChange change) {
        String resource = switch (change.kind()) {
            case RESERVATION -> "reservations";
            case FOOD_ORDER -> "food-orders";
        };

        // Mỗi lần gửi có try/catch riêng; một kênh lỗi không chặn kênh kia.
        sendSafely(() -> messaging.convertAndSend(
                "/topic/staff/" + resource, change.payload()), change);

        if (change.customerId() != null) {
            sendSafely(() -> messaging.convertAndSendToUser(
                    change.customerId().toString(),
                    "/queue/" + resource,
                    change.payload()), change);
        }
    }

    private void sendSafely(Runnable send, RealtimeChange change) {
        try {
            send.run();
        } catch (RuntimeException exception) {
            log.error("Realtime delivery failed, eventId={}",
                    change.payload().eventId(), exception);
        }
    }
}