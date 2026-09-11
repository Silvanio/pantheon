package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskCardAssignee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCardAssigneeRepository extends JpaRepository<TaskCardAssignee, UUID> {

    List<TaskCardAssignee> findByCardIdIn(List<UUID> cardIds);

    Optional<TaskCardAssignee> findByCardIdAndSiteMembershipId(UUID cardId, UUID siteMembershipId);

    void deleteByCardId(UUID cardId);
}
