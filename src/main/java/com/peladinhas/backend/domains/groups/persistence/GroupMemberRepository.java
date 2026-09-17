package com.peladinhas.backend.domains.groups.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {

    boolean existsByGroup_IdAndUser_IdAndRoleAndStatus(
            UUID groupId,
            UUID userId,
            GroupMemberRole role,
            GroupMemberStatus status);
}
