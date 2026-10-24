package  com.sagares.saga_res_api.reservation.entity;

public enum ReservationStatus {
    PENDING, //Chờ duyệt
    CONFIRMED, //Đã xác nhận
    ARRIVED, //Đã đến
    COMPLETED, // Hoàn thành
    NO_SHOW, //Không đến
    CANCELLED, //Đã hủy
    REJECTED //Từ chôí
}
