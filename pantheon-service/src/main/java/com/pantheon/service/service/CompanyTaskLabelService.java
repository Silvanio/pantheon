package com.pantheon.service.service;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.NotCompanyMemberException;
import com.pantheon.service.exception.TaskLabelNotFoundException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Company-admin-only CRUD for the Tasks board's predefined, reusable label catalog. Not gated by
 * {@link SitePermissionService} — this is a fixed, company-admin-only feature configured outside
 * any obra, mirroring {@link TaskColumnService}. See {@code company-task-labels}.
 */
@Service
public class CompanyTaskLabelService {

    private final TaskLabelRepository labelRepository;
    private final TaskCardLabelRepository cardLabelRepository;
    private final CompanyMembershipRepository membershipRepository;

    public CompanyTaskLabelService(
            TaskLabelRepository labelRepository, TaskCardLabelRepository cardLabelRepository,
            CompanyMembershipRepository membershipRepository) {
        this.labelRepository = labelRepository;
        this.cardLabelRepository = cardLabelRepository;
        this.membershipRepository = membershipRepository;
    }

    public List<TaskLabel> list(UUID companyId, UUID actingUserId) {
        requireMembership(companyId, actingUserId);
        return labelRepository.findByCompanyIdOrderByNameAsc(companyId);
    }

    @Transactional
    public TaskLabel create(UUID companyId, UUID actingUserId, TaskLabelRequest request) {
        requireAdmin(companyId, actingUserId);
        return labelRepository.save(
                TaskLabel.predefined(UUID.randomUUID(), companyId, request.name(), request.colorHex(), Instant.now()));
    }

    @Transactional
    public void delete(UUID labelId, UUID actingUserId) {
        TaskLabel label = requireLabel(labelId);
        requireAdmin(label.getCompanyId(), actingUserId);

        cardLabelRepository.deleteAll(cardLabelRepository.findByLabelId(labelId));
        labelRepository.delete(label);
    }

    private TaskLabel requireLabel(UUID labelId) {
        return labelRepository.findById(labelId).orElseThrow(() -> new TaskLabelNotFoundException(labelId));
    }

    private void requireAdmin(UUID companyId, UUID userId) {
        CompanyMembership membership = membershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyAdminException(companyId));
        if (membership.getRole() != CompanyRole.ADMIN) {
            throw new NotCompanyAdminException(companyId);
        }
    }

    private void requireMembership(UUID companyId, UUID userId) {
        membershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyMemberException(companyId));
    }
}
