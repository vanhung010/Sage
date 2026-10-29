package com.sagares.saga_res_api.common.realtime;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WsSecurity implements ChannelInterceptor {
    private final JwtService jwtService;
    private final AccountRepository accounts;

    private record Connection(WebSocketSession socket, Instant openedAt) {}

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Map<String, WsUser> users = new ConcurrentHashMap<>();

    public void connected(WebSocketSession socket) {
        connections.put(socket.getId(), new Connection(socket, Instant.now()));
    }

    public void disconnected(String sessionId) {
        users.remove(sessionId);
        connections.remove(sessionId);
    }

    private boolean valid(WsUser user) {
        if (user == null || !Instant.now().isBefore(user.expiresAt())) {
            return false;
        }

        // Đọc DB hiện tại; không chỉ tin role trong JWT lúc CONNECT.
        return accounts.findById(user.accountId())
                .filter(account -> !account.isLocked())
                .filter(account -> account.getRole() == user.role())
                .isPresent();
    }

    private void close(String sessionId) {
        if (sessionId == null) return;
        Connection connection = connections.remove(sessionId);
        users.remove(sessionId);
        if (connection == null) return;
        try {
            connection.socket().close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {
            // Socket đã mất kết nối: state đã được dọn.
        }
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getSessionId() == null) {
            throw new AccessDeniedException("Invalid WebSocket session");
        }

        String sessionId = accessor.getSessionId();
        StompCommand command = accessor.getCommand();

        // Cho phép đóng cả phiên chưa CONNECT thành công.
        if (command == StompCommand.DISCONNECT) return message;

        try {
            if (command == StompCommand.CONNECT) {
                String header = accessor.getFirstNativeHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ")) {
                    throw new BadCredentialsException("Authentication required");
                }

                var claims = jwtService.parseAndValidate(header.substring(7));
                WsUser user = new WsUser(
                        claims.accountId(), claims.role(), claims.expiresAt());

                if (!connections.containsKey(sessionId) || !valid(user)
                        || users.putIfAbsent(sessionId, user) != null) {
                    throw new BadCredentialsException("Invalid session");
                }

                accessor.setUser(user);
                return message;
            }

            WsUser user = users.get(sessionId);
            if (!valid(user)) {
                throw new BadCredentialsException("Invalid session");
            }

            if (command == StompCommand.SUBSCRIBE) {
                String destination = accessor.getDestination();
                boolean allowed = switch (user.role()) {
                    case STAFF, ADMIN ->
                            "/topic/staff/reservations".equals(destination)
                                    || "/topic/staff/food-orders".equals(destination);
                    case CUSTOMER ->
                            "/user/queue/reservations".equals(destination)
                                    || "/user/queue/food-orders".equals(destination);
                };
                if (!allowed) {
                    throw new AccessDeniedException("Subscription denied");
                }
                return message;
            }

            // command null là heartbeat. Dùng auto-ack, không nhận ACK/NACK.
            if (command == null || command == StompCommand.UNSUBSCRIBE) {
                return message;
            }

            // Chặn SEND và mọi command ngoài danh sách cho phép.
            throw new AccessDeniedException("Command denied");
        } catch (RuntimeException exception) {
            close(sessionId);
            // Không gửi chi tiết lỗi JWT/DB cho client.
            throw new AccessDeniedException("WebSocket access denied");
        }
    }

    public ChannelInterceptor outboundGuard() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders())
                        != SimpMessageType.MESSAGE) {
                    return message;
                }

                String sessionId = SimpMessageHeaderAccessor.getSessionId(
                        message.getHeaders());
                if (sessionId == null) return null;

                try {
                    if (valid(users.get(sessionId))) return message;
                } catch (RuntimeException ignored) {
                    // Fail closed nếu không kiểm tra được quyền.
                }
                close(sessionId);
                return null;
            }
        };
    }

    // SchedulingConfig trong Sage đã bật @EnableScheduling.
    // Không có frame mới thì dọn phiên quá hạn/khóa trong tối đa khoảng 10 giây.
    // Mọi frame ứng dụng vẫn được kiểm tra ngay ở inbound/outbound.
    @Scheduled(fixedDelay = 10000)
    public void cleanInvalidSessions() {
        for (var entry : connections.entrySet()) {
            String sessionId = entry.getKey();
            WsUser user = users.get(sessionId);
            try {
                if (user == null) {
                    if (entry.getValue().openedAt().plusSeconds(10)
                            .isBefore(Instant.now())) {
                        close(sessionId);
                    }
                } else if (!valid(user)) {
                    close(sessionId);
                }
            } catch (RuntimeException ignored) {
                close(sessionId);
            }
        }
    }
}