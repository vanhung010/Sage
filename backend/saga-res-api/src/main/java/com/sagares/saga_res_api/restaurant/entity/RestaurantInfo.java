package  com.sagares.saga_res_api.restaurant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "restaurant_info")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantInfo {

    @Id
    private Short id;

    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private String address;

    private String phone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestaurantInfo other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
