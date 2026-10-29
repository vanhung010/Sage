package com.sagares.saga_res_api.common.realtime;

// Event nội bộ; customerId không được gửi trong payload WebSocket.

public record RealtimeChange(
        Kind kind,
        Long customerId,
        RealtimeEvent payload
) {
    public enum Kind { RESERVATION, FOOD_ORDER }
}