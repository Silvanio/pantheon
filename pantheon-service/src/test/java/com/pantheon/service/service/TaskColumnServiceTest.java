package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.TaskColumnRequest;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.TaskColumnInUseException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskColumnServiceTest {

    @Mock
    private TaskColumnRepository columnRepository;

    @Mock
    private TaskCardRepository cardRepository;

    @Mock
    private CompanyMembershipRepository membershipRepository;

    private TaskColumnService service;

    private UUID companyId;
    private UUID adminUserId;
    private UUID memberUserId;

    @BeforeEach
    void setUp() {
        service = new TaskColumnService(columnRepository, cardRepository, membershipRepository);
        companyId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();

        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, adminUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, adminUserId, CompanyRole.ADMIN, Instant.now())));
        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, memberUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, memberUserId, CompanyRole.MEMBER, Instant.now())));
    }

    @Test
    void adminCanCreateColumn() {
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        when(columnRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskColumn column = service.create(companyId, adminUserId, new TaskColumnRequest("A Fazer"));

        assertThat(column.getName()).isEqualTo("A Fazer");
        assertThat(column.getCompanyId()).isEqualTo(companyId);
        assertThat(column.getSortOrder()).isZero();
    }

    @Test
    void nonAdminBlockedFromCreatingColumn() {
        assertThatThrownBy(() -> service.create(companyId, memberUserId, new TaskColumnRequest("A Fazer")))
                .isInstanceOf(NotCompanyAdminException.class);
        verify(columnRepository, never()).save(any());
    }

    @Test
    void deleteBlockedWhenColumnHasCards() {
        UUID columnId = UUID.randomUUID();
        TaskColumn column = new TaskColumn(columnId, companyId, "Em Andamento", 0, Instant.now());
        when(columnRepository.findById(columnId)).thenReturn(Optional.of(column));
        when(cardRepository.existsByColumnId(columnId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(columnId, adminUserId)).isInstanceOf(TaskColumnInUseException.class);
        verify(columnRepository, never()).delete(any());
    }

    @Test
    void deleteSucceedsWhenColumnEmpty() {
        UUID columnId = UUID.randomUUID();
        TaskColumn column = new TaskColumn(columnId, companyId, "Concluido", 0, Instant.now());
        when(columnRepository.findById(columnId)).thenReturn(Optional.of(column));
        when(cardRepository.existsByColumnId(columnId)).thenReturn(false);

        service.delete(columnId, adminUserId);

        verify(columnRepository).delete(column);
    }
}
