package com.sagares.saga_res_api.auth.repository;

import com.sagares.saga_res_api.auth.entity.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetRequestRepository
        extends JpaRepository<PasswordResetRequest, Long> {

    Optional<PasswordResetRequest> findFirstByAccount_IdOrderByCreatedAtDesc(Long accountId);

    long countByAccount_IdAndCreatedAtAfter(
            Long accountId,
            Instant createdAfter
    );

    Optional<PasswordResetRequest> findByAccount_IdAndUsedAtIsNullAndRevokedAtIsNull(Long accountId);

    Optional<PasswordResetRequest> findByResetTokenHash(String resetTokenHash);

    @Query("""
            select request.account.id
            from PasswordResetRequest request
            where request.resetTokenHash = :tokenHash
            """)
    Optional<Long> findAccountIdByTokenHash(
            @Param("tokenHash") String tokenHash
    );
}