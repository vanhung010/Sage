package  com.sagares.saga_res_api.menu.repository;


import com.sagares.saga_res_api.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Category> findAllByOrderByDisplayOrderAscNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
