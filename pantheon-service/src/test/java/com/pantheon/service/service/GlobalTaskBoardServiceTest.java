package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalTaskBoardServiceTest {

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private TaskColumnRepository columnRepository;

    @Mock
    private TaskCardRepository cardRepository;

    @Mock
    private TaskCardLabelRepository cardLabelRepository;

    @Mock
    private CompanyMembershipRepository membershipRepository;

    private GlobalTaskBoardService service;

    private UUID companyId;
    private UUID adminUserId;
    private UUID memberUserId;

    @BeforeEach
    void setUp() {
        service = new GlobalTaskBoardService(siteRepository, columnRepository, cardRepository, cardLabelRepository, membershipRepository);
        companyId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();

        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, adminUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, adminUserId, CompanyRole.ADMIN, Instant.now())));
        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, memberUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, memberUserId, CompanyRole.MEMBER, Instant.now())));
    }

    @Test
    void nonAdminBlockedFromGlobalBoard() {
        assertThatThrownBy(() -> service.build(companyId, memberUserId)).isInstanceOf(NotCompanyAdminException.class);
    }

    @Test
    void adminSeesCardsFromEverySite() {
        UUID siteAId = UUID.randomUUID();
        UUID siteBId = UUID.randomUUID();
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(
                site(siteAId, companyId, "Obra A"), site(siteBId, companyId, "Obra B")));
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        TaskCard cardA = new TaskCard(UUID.randomUUID(), siteAId, UUID.randomUUID(), "Card A", null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        TaskCard cardB = new TaskCard(UUID.randomUUID(), siteBId, UUID.randomUUID(), "Card B", null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findByConstructionSiteIdInOrderBySortOrderAsc(any())).thenReturn(List.of(cardA, cardB));
        when(cardLabelRepository.findByCardIdIn(any())).thenReturn(List.of());

        var board = service.build(companyId, adminUserId);

        assertThat(board.cards()).containsExactly(cardA, cardB);
        assertThat(board.siteById()).containsKeys(siteAId, siteBId);
    }

    @Test
    void colorForIsStableAcrossCalls() {
        UUID siteId = UUID.randomUUID();

        assertThat(GlobalTaskBoardService.colorFor(siteId)).isEqualTo(GlobalTaskBoardService.colorFor(siteId));
    }

    private ConstructionSite site(UUID id, UUID companyId, String name) {
        return new ConstructionSite(id, companyId, name, "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }
}
