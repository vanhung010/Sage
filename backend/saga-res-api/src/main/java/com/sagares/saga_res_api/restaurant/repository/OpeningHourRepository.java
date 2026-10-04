package com.sagares.saga_res_api.restaurant.repository;


import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpeningHourRepository extends JpaRepository<OpeningHour, Short> {
}
