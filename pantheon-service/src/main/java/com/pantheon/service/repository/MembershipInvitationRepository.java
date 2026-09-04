package com.pantheon.service.repository;

import com.pantheon.service.entity.MembershipInvitation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipInvitationRepository extends JpaRepository<MembershipInvitation, UUID> {

    Optional<MembershipInvitation> findByTokenHash(String tokenHash);

    Optional<MembershipInvitation> findByMembershipId(UUID membershipId);
}
