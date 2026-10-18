package com.sagares.saga_res_api.reservation.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class ReservationMailListener {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Executor executor;
    private final String from;

    public ReservationMailListener(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Qualifier("reservationMailExecutor") Executor executor,
            @Value("${app.mail.from:}") String from
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.executor = executor;
        this.from = from;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmed(ReservationConfirmedEvent event) {
        // Chỉ chạy nếu transaction tạo reservation đã commit thành công.
        try {
            executor.execute(() -> sendEmail(event));
        } catch (RuntimeException exception) {
            // Queue đầy hoặc executor dừng cũng không làm API báo
            // đặt bàn thất bại khi đơn thực tế đã được lưu.
            log.error(
                    "Không thể xếp lịch gửi email cho đơn {}",
                    event.reservation().code(),
                    exception
            );
        }
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRescheduled(ReservationRescheduledEvent event) {
        try {
            executor.execute(() -> sendRescheduledEmail(event));
        } catch (RuntimeException exception) {
            log.error(
                    "Không thể xếp lịch gửi email đổi lịch cho đơn {}",
                    event.reservation().code(),
                    exception
            );
        }
    }

    private void sendRescheduledEmail(ReservationRescheduledEvent event) {
        try {
            JavaMailSender sender = mailSenderProvider.getIfAvailable();

            if (sender == null || from.isBlank()) {
                log.warn(
                        "Chưa cấu hình SMTP; không gửi email đổi lịch cho đơn {}",
                        event.reservation().code()
                );
                return;
            }

            var reservation = event.reservation();

            String status = reservation.status()
                    == com.sagares.saga_res_api.reservation.entity
                    .ReservationStatus.PENDING
                    ? "Đơn đang chờ nhà hàng xác nhận."
                    : "Lịch mới đã được xác nhận.";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(event.recipient());

            message.setSubject(
                    "Sage - Đổi lịch đặt bàn " + reservation.code()
            );

            message.setText("""
                Đơn đặt bàn của bạn đã được đổi lịch.
                Mã đặt bàn: %s
                Số khách: %d
                Bắt đầu: %s
                Kết thúc: %s
                %s
                Thời gian hiển thị theo múi giờ Việt Nam.
                """.formatted(
                    reservation.code(),
                    reservation.partySize(),
                    reservation.startTime().format(DATE_TIME_FORMAT),
                    reservation.endTime().format(DATE_TIME_FORMAT),
                    status
            ));

            sender.send(message);

        } catch (Exception exception) {
            log.error(
                    "Gửi email đổi lịch thất bại cho đơn {}",
                    event.reservation().code(),
                    exception
            );
        }
    }

    private void sendEmail(ReservationConfirmedEvent event) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

            if (mailSender == null || from.isBlank()) {
                log.warn(
                        "Chưa cấu hình SMTP hoặc app.mail.from; "
                                + "không gửi email cho đơn {}",
                        event.reservation().code()
                );
                return;
            }

            var reservation = event.reservation();

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(from);
            message.setTo(event.recipient());
            message.setSubject(
                    "Sage - Xác nhận đặt bàn " + reservation.code()
            );

            message.setText("""
                    Đặt bàn của bạn đã được xác nhận.

                    Mã đặt bàn: %s
                    Số người: %d
                    Bắt đầu: %s
                    Kết thúc: %s

                    Thời gian hiển thị theo múi giờ Việt Nam.
                    Sage hẹn gặp bạn!
                    """.formatted(
                    reservation.code(),
                    reservation.partySize(),
                    reservation.startTime().format(DATE_TIME_FORMAT),
                    reservation.endTime().format(DATE_TIME_FORMAT)
            ));

            mailSender.send(message);

        } catch (Exception exception) {
            // Email là tác vụ sau commit.
            // Gửi lỗi chỉ ghi log; reservation vẫn hợp lệ.
            log.error(
                    "Gửi email xác nhận thất bại cho đơn {}",
                    event.reservation().code(),
                    exception
            );
        }
    }
}