package com.sushishop.ai;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderDraftRepository extends JpaRepository<OrderDraft, UUID> {

    Optional<OrderDraft> findBySessionId(UUID sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT draft FROM OrderDraft draft
            WHERE draft.session.id = :sessionId
              AND draft.session.user.email = :email
            """)
    Optional<OrderDraft> findOwnedForUpdate(@Param("sessionId") UUID sessionId,
                                            @Param("email") String email);
}
