package com.sagares.saga_res_api.restaurant.repository;

import com.sagares.saga_res_api.restaurant.entity.RestaurantInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantInfoRepository extends JpaRepository<RestaurantInfo, Short> {
}
