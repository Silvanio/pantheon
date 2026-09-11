package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskLabelRepository;
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
class CompanyTaskLabelServiceTest {

    @Mock
    private TaskLabelRepository labelRepository;

    @Mock
    private TaskCardLabelRepository cardLabelRepository;

    @Mock
    private CompanyMembershipRepository membershipRepository;

    private CompanyTaskLabelService service;

    private UUID companyId;
    private UUID adminUserId;
    private UUID memberUserId;

    @BeforeEach
    void setUp() {
        service = new CompanyTaskLabelService(labelRepository, cardLabelRepository, membershipRepository);
        companyId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();

        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, adminUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, adminUserId, CompanyRole.ADMIN, Instant.now())));
        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, memberUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, memberUserId, CompanyRole.MEMBER, Instant.now())));
    }

    @Test
    void adminCanCreatePredefinedLabel() {
        when(labelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskLabel label = service.create(companyId, adminUserId, new TaskLabelRequest("Urgente", "#EF4444"));

        assertThat(label.getName()).isEqualTo("Urgente");
        assertThat(label.getCompanyId()).isEqualTo(companyId);
        assertThat(label.isPredefined()).isTrue();
    }

    @Test
    void nonAdminBlockedFromCreatingLabel() {
        assertThatThrownBy(() -> service.create(companyId, memberUserId, new TaskLabelRequest("Urgente", "#EF4444")))
                .isInstanceOf(NotCompanyAdminException.class);
        verify(labelRepository, never()).save(any());
    }

    @Test
    void memberCanListCatalogWithoutBeingAdmin() {
        when(labelRepository.findByCompanyIdOrderByNameAsc(companyId)).thenReturn(List.of());

        var result = service.list(companyId, memberUserId);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteRemovesLabelAndItsAttachments() {
        UUID labelId = UUID.randomUUID();
        TaskLabel label = TaskLabel.predefined(labelId, companyId, "Urgente", "#EF4444", Instant.now());
        when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));
        var attachment = new com.pantheon.service.entity.TaskCardLabel(UUID.randomUUID(), UUID.randomUUID(), labelId);
        when(cardLabelRepository.findByLabelId(labelId)).thenReturn(List.of(attachment));

        service.delete(labelId, adminUserId);

        verify(cardLabelRepository).deleteAll(List.of(attachment));
        verify(labelRepository).delete(label);
    }

    @Test
    void nonAdminBlockedFromDeletingLabel() {
        UUID labelId = UUID.randomUUID();
        TaskLabel label = TaskLabel.predefined(labelId, companyId, "Urgente", "#EF4444", Instant.now());
        when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));

        assertThatThrownBy(() -> service.delete(labelId, memberUserId)).isInstanceOf(NotCompanyAdminException.class);
        verify(labelRepository, never()).delete(any());
    }
}
