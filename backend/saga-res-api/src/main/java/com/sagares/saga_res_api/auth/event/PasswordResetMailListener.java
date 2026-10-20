package com.sagares.saga_res_api.auth.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Component
@Slf4j
public class PasswordResetMailListener {

    private final JavaMailSender mailSender;
    private final Executor executor;
    private final String from;

    public PasswordResetMailListener(
            JavaMailSender mailSender,
            @Qualifier("passwordResetExecutor") Executor executor,
            @Value("${app.mail.from}") String from
    ) {
        if (from.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình app.mail.from");
        }

        this.mailSender = mailSender;
        this.executor = executor;
        this.from = from;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMailRequested(PasswordResetMailEvent event) {
        try {
            executor.execute(() -> send(event));
        } catch (RejectedExecutionException exception) {
            log.error("Hàng đợi email khôi phục mật khẩu đã đầy");
        }
    }

    private void send(PasswordResetMailEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(from);
            message.setTo(event.recipient());
            message.setSubject(event.subject());
            message.setText(event.body());

            mailSender.send(message);
        } catch (Exception exception) {
            // Không ghi event/body vì chứa mã xác nhận.
            log.error(
                    "Gửi email khôi phục mật khẩu thất bại: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}