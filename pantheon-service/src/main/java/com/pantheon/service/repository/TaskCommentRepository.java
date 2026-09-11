package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    List<TaskComment> findByCardIdOrderByCreatedAtAsc(UUID cardId);

    void deleteByCardId(UUID cardId);

    @Query("select tc.cardId as cardId, count(tc) as commentCount from TaskComment tc "
            + "where tc.cardId in :cardIds group by tc.cardId")
    List<CardCommentCount> countByCardIdIn(@Param("cardIds") List<UUID> cardIds);

    interface CardCommentCount {
        UUID getCardId();

        long getCommentCount();
    }
}
