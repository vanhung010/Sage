package  com.sagares.saga_res_api.restaurant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "opening_hour")
@Getter
@Setter
@NoArgsConstructor
public class OpeningHour {

    @Id
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpeningHour other)) return false;
        return dayOfWeek != null && dayOfWeek.equals(other.dayOfWeek);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
