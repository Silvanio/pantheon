package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskCardLabel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCardLabelRepository extends JpaRepository<TaskCardLabel, UUID> {

    List<TaskCardLabel> findByCardIdIn(List<UUID> cardIds);

    Optional<TaskCardLabel> findByCardIdAndLabelId(UUID cardId, UUID labelId);
}
