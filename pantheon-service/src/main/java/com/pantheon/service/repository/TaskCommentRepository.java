package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    List<TaskComment> findByCardIdOrderByCreatedAtAsc(UUID cardId);
}
