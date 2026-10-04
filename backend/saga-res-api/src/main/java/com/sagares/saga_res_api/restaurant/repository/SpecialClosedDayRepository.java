package com.sagares.saga_res_api.restaurant.repository;

import com.sagares.saga_res_api.restaurant.entity.SpecialClosedDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SpecialClosedDayRepository extends JpaRepository<SpecialClosedDay, Long> {

    Optional<SpecialClosedDay> findByClosedDate(LocalDate closedDate);

    boolean existsByClosedDate(LocalDate closedDate);
}
