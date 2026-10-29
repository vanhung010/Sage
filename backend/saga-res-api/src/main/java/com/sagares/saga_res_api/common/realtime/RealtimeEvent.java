package com.sagares.saga_res_api.common.realtime;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent(
        UUID eventId,
        String type,
        Long resourceId,
        Instant occurredAt
) {
    public static RealtimeEvent of(String type, Long resourceId) {
        return new RealtimeEvent(UUID.randomUUID(), type, resourceId, Instant.now());
    }
}