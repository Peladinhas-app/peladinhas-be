package com.peladinhas.backend.domains.groups.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberId;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRole;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberStatus;
import com.peladinhas.backend.domains.groups.persistence.GroupRepository;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ContextualPermissionDeniedException;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {

    private final Clock clock;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(
            final Clock clock,
            final GroupRepository groupRepository,
            final GroupMemberRepository groupMemberRepository,
            final UserRepository userRepository) {
        this.clock = clock;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupEntity createGroup(final CreateGroupCommand command) {
        UserEntity creator = requireUser(command.creatorUserId());
        OffsetDateTime now = OffsetDateTime.now(clock);

        GroupEntity group = new GroupEntity();
        group.setId(UUID.randomUUID());
        group.setName(command.name());
        group.setDescription(command.description());
        group.setVisibility(command.visibility());
        group.setCreatedByUser(creator);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);

        GroupEntity savedGroup = groupRepository.save(group);
        addActiveAdmin(savedGroup, creator, now);
        return savedGroup;
    }

    @Transactional(readOnly = true)
    public GroupEntity requireGroup(final UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group was not found."));
    }

    @Transactional(readOnly = true)
    public void requireActiveGroupAdmin(final UUID groupId, final UUID userId) {
        boolean isAdmin = groupMemberRepository.existsByGroup_IdAndUser_IdAndRoleAndStatus(
                groupId,
                userId,
                GroupMemberRole.ADMIN,
                GroupMemberStatus.ACTIVE);
        if (!isAdmin) {
            throw new ContextualPermissionDeniedException("User is not an active group admin.");
        }
    }

    private UserEntity requireUser(final UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }

    private void addActiveAdmin(final GroupEntity group, final UserEntity user, final OffsetDateTime now) {
        GroupMemberEntity member = new GroupMemberEntity();
        member.setId(new GroupMemberId(group.getId(), user.getId()));
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMemberRole.ADMIN);
        member.setStatus(GroupMemberStatus.ACTIVE);
        member.setJoinedAt(now);
        member.setUpdatedAt(now);
        groupMemberRepository.save(member);
    }
}
