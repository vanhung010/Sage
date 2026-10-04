package  com.sagares.saga_res_api.menu.repository;


import com.sagares.saga_res_api.menu.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
