package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.MaterialRequestCreationRequest;
import com.pantheon.service.dto.MaterialRequestItemCreationRequest;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.MaterialRequest;
import com.pantheon.service.entity.MaterialRequestItem;
import com.pantheon.service.entity.MaterialRequestStatus;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.entity.ReceiptVerification;
import com.pantheon.service.exception.DuplicateReceiptVerificationException;
import com.pantheon.service.exception.MaterialRequestNotApprovedException;
import com.pantheon.service.exception.MaterialRequestNotPendingException;
import com.pantheon.service.exception.NotMaterialRequestApproverException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialRequestItemRepository;
import com.pantheon.service.repository.MaterialRequestRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.repository.ReceiptVerificationRepository;
import java.math.BigDecimal;
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
class MaterialRequestServiceTest {

    @Mock
    private MaterialRequestRepository requestRepository;

    @Mock
    private MaterialRequestItemRepository itemRepository;

    @Mock
    private ReceiptVerificationRepository verificationRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    private MaterialRequestService materialRequestService;

    private UUID projectId;
    private UUID userId;
    private ConstructionSite site;

    @BeforeEach
    void setUp() {
        materialRequestService = new MaterialRequestService(
                requestRepository, itemRepository, verificationRepository, siteRepository, membershipRepository);

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        site = new ConstructionSite(
                UUID.randomUUID(), projectId, "Torre Norte", "Av. Central, 500", LocalDate.now(), null, userId,
                Instant.now());

        lenient().when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        lenient().when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(verificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ProjectMembership member(ProjectRole role, ConstructionFunction function) {
        return new ProjectMembership(UUID.randomUUID(), projectId, userId, role, function, null, Instant.now());
    }

    @Test
    void createPersistsRequestInPendingStatusWithItems() {
        ProjectMembership member = member(ProjectRole.MEMBER, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        MaterialRequest request = materialRequestService.create(
                site.getId(), userId,
                new MaterialRequestCreationRequest(
                        List.of(new MaterialRequestItemCreationRequest(UUID.randomUUID(), new BigDecimal("10")))));

        assertThat(request.getStatus()).isEqualTo(MaterialRequestStatus.PENDING);
        assertThat(request.getRequestedBy()).isEqualTo(userId);
    }

    @Test
    void approveAllowedForAdmin() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership admin = member(ProjectRole.ADMIN, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(admin));

        MaterialRequest approved = materialRequestService.approve(request.getId(), userId);

        assertThat(approved.getStatus()).isEqualTo(MaterialRequestStatus.APPROVED);
    }

    @Test
    void approveAllowedForEngineer() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership engineer = member(ProjectRole.MEMBER, ConstructionFunction.ENGINEER);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(engineer));

        MaterialRequest approved = materialRequestService.approve(request.getId(), userId);

        assertThat(approved.getStatus()).isEqualTo(MaterialRequestStatus.APPROVED);
    }

    @Test
    void approveRejectedForOtherMembers() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership member = member(ProjectRole.MEMBER, ConstructionFunction.SITE_FOREMAN);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> materialRequestService.approve(request.getId(), userId))
                .isInstanceOf(NotMaterialRequestApproverException.class);
    }

    @Test
    void approveRejectedWhenNotPending() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        request.reject(UUID.randomUUID(), "motivo", Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership admin = member(ProjectRole.ADMIN, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> materialRequestService.approve(request.getId(), userId))
                .isInstanceOf(MaterialRequestNotPendingException.class);
    }

    @Test
    void rejectWithReasonTransitionsStatus() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership admin = member(ProjectRole.ADMIN, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(admin));

        MaterialRequest rejected = materialRequestService.reject(request.getId(), userId, "Fora do orçamento");

        assertThat(rejected.getStatus()).isEqualTo(MaterialRequestStatus.REJECTED);
        assertThat(rejected.getDecisionNote()).isEqualTo("Fora do orçamento");
    }

    @Test
    void rejectWithoutReasonRejected() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership admin = member(ProjectRole.ADMIN, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> materialRequestService.reject(request.getId(), userId, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordVerificationRejectedWhenRequestNotApproved() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership member = member(ProjectRole.MEMBER, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> materialRequestService.recordVerification(
                        request.getId(), userId, UUID.randomUUID(), BigDecimal.TEN, null))
                .isInstanceOf(MaterialRequestNotApprovedException.class);
    }

    @Test
    void recordVerificationSetsPartiallyReceivedWithRemainingItems() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        request.approve(UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership member = member(ProjectRole.MEMBER, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        MaterialRequestItem item1 = new MaterialRequestItem(UUID.randomUUID(), request.getId(), UUID.randomUUID(), BigDecimal.TEN);
        MaterialRequestItem item2 = new MaterialRequestItem(UUID.randomUUID(), request.getId(), UUID.randomUUID(), BigDecimal.TEN);
        when(itemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(verificationRepository.findByMaterialRequestItemId(item1.getId())).thenReturn(Optional.empty());
        when(itemRepository.findByMaterialRequestId(request.getId())).thenReturn(List.of(item1, item2));

        ReceiptVerification firstVerification = new ReceiptVerification(
                UUID.randomUUID(), item1.getId(), BigDecimal.TEN, userId, null, Instant.now());
        when(verificationRepository.findByMaterialRequestItemIdIn(List.of(item1.getId(), item2.getId())))
                .thenReturn(List.of(firstVerification));

        materialRequestService.recordVerification(request.getId(), userId, item1.getId(), BigDecimal.TEN, null);

        assertThat(request.getStatus()).isEqualTo(MaterialRequestStatus.PARTIALLY_RECEIVED);
    }

    @Test
    void recordVerificationSetsReceivedWhenAllItemsVerified() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        request.approve(UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership member = member(ProjectRole.MEMBER, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        MaterialRequestItem item1 = new MaterialRequestItem(UUID.randomUUID(), request.getId(), UUID.randomUUID(), BigDecimal.TEN);
        when(itemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(verificationRepository.findByMaterialRequestItemId(item1.getId())).thenReturn(Optional.empty());
        when(itemRepository.findByMaterialRequestId(request.getId())).thenReturn(List.of(item1));

        ReceiptVerification verification = new ReceiptVerification(
                UUID.randomUUID(), item1.getId(), new BigDecimal("8"), userId, "Divergência: 2 unidades a menos", Instant.now());
        when(verificationRepository.findByMaterialRequestItemIdIn(List.of(item1.getId()))).thenReturn(List.of(verification));

        materialRequestService.recordVerification(request.getId(), userId, item1.getId(), new BigDecimal("8"), "Divergência");

        assertThat(request.getStatus()).isEqualTo(MaterialRequestStatus.RECEIVED);
    }

    @Test
    void recordVerificationRejectedWhenAlreadyVerified() {
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), UUID.randomUUID(), Instant.now());
        request.approve(UUID.randomUUID(), Instant.now());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        ProjectMembership member = member(ProjectRole.MEMBER, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        MaterialRequestItem item1 = new MaterialRequestItem(UUID.randomUUID(), request.getId(), UUID.randomUUID(), BigDecimal.TEN);
        when(itemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(verificationRepository.findByMaterialRequestItemId(item1.getId()))
                .thenReturn(Optional.of(new ReceiptVerification(
                        UUID.randomUUID(), item1.getId(), BigDecimal.TEN, userId, null, Instant.now())));

        assertThatThrownBy(() -> materialRequestService.recordVerification(
                        request.getId(), userId, item1.getId(), BigDecimal.TEN, null))
                .isInstanceOf(DuplicateReceiptVerificationException.class);
    }

    @Test
    void listReturnsRequestsForSite() {
        ProjectMembership member = member(ProjectRole.MEMBER, null);
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));
        MaterialRequest request = new MaterialRequest(UUID.randomUUID(), site.getId(), userId, Instant.now());
        when(requestRepository.findByConstructionSiteIdOrderByCreatedAtDesc(site.getId())).thenReturn(List.of(request));

        assertThat(materialRequestService.list(site.getId(), userId, null)).containsExactly(request);
    }
}
