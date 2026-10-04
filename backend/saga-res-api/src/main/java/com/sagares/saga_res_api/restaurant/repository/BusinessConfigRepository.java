package  com.sagares.saga_res_api.restaurant.repository;


import com.sagares.saga_res_api.restaurant.entity.BusinessConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessConfigRepository extends JpaRepository<BusinessConfig, String> {
}
