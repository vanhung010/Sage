package com.sagares.saga_res_api.restaurant.repository;


import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalTime;
import java.util.Optional;

public interface OpeningHourRepository extends JpaRepository<OpeningHour, Short> {

    // Assigned weekday IDs must be inserted, never merged over an existing schedule.
    @Modifying
    @Query(value = """
            INSERT INTO opening_hour (day_of_week, open_time, close_time, is_closed)
            VALUES (:dayOfWeek, :openTime, :closeTime, false)
            ON CONFLICT (day_of_week) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("dayOfWeek") Short dayOfWeek,
                       @Param("openTime") LocalTime openTime,
                       @Param("closeTime") LocalTime closeTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from OpeningHour h where h.dayOfWeek = :id")
    Optional<OpeningHour> findByIdForUpdate(@Param("id") Short id);
}
