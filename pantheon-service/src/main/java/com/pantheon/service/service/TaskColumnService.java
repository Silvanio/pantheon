package com.pantheon.service.service;

import com.pantheon.service.dto.TaskColumnRequest;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.NotCompanyMemberException;
import com.pantheon.service.exception.TaskColumnInUseException;
import com.pantheon.service.exception.TaskColumnNotFoundException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Company-admin-only CRUD for the Tasks board's shared columns. Not gated by
 * {@link SitePermissionService} — this is a fixed, company-admin-only feature configured outside
 * any obra. See {@code company-task-columns}.
 */
@Service
public class TaskColumnService {

    private final TaskColumnRepository columnRepository;
    private final TaskCardRepository cardRepository;
    private final CompanyMembershipRepository membershipRepository;

    public TaskColumnService(
            TaskColumnRepository columnRepository, TaskCardRepository cardRepository,
            CompanyMembershipRepository membershipRepository) {
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.membershipRepository = membershipRepository;
    }

    public List<TaskColumn> list(UUID companyId, UUID actingUserId) {
        requireMembership(companyId, actingUserId);
        return columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
    }

    @Transactional
    public TaskColumn create(UUID companyId, UUID actingUserId, TaskColumnRequest request) {
        requireAdmin(companyId, actingUserId);
        int nextSortOrder = columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId).size();
        return columnRepository.save(new TaskColumn(UUID.randomUUID(), companyId, request.name(), nextSortOrder, Instant.now()));
    }

    @Transactional
    public TaskColumn rename(UUID columnId, UUID actingUserId, TaskColumnRequest request) {
        TaskColumn column = requireColumn(columnId);
        requireAdmin(column.getCompanyId(), actingUserId);
        column.rename(request.name());
        return columnRepository.save(column);
    }

    @Transactional
    public TaskColumn reorder(UUID columnId, UUID actingUserId, int sortOrder) {
        TaskColumn column = requireColumn(columnId);
        requireAdmin(column.getCompanyId(), actingUserId);
        column.reorder(sortOrder);
        return columnRepository.save(column);
    }

    @Transactional
    public void delete(UUID columnId, UUID actingUserId) {
        TaskColumn column = requireColumn(columnId);
        requireAdmin(column.getCompanyId(), actingUserId);
        if (cardRepository.existsByColumnId(columnId)) {
            throw new TaskColumnInUseException(columnId);
        }
        columnRepository.delete(column);
    }

    private TaskColumn requireColumn(UUID columnId) {
        return columnRepository.findById(columnId).orElseThrow(() -> new TaskColumnNotFoundException(columnId));
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
