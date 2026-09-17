package com.peladinhas.backend.domains.groups.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_members")
public class GroupMemberEntity {

    @EmbeddedId
    private GroupMemberId id = new GroupMemberId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Convert(converter = GroupMemberRole.ConverterImpl.class)
    @Column(name = "role", nullable = false, columnDefinition = "varchar")
    private GroupMemberRole role;

    @Convert(converter = GroupMemberStatus.ConverterImpl.class)
    @Column(name = "status", nullable = false, columnDefinition = "varchar")
    private GroupMemberStatus status;

    @Column(name = "joined_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime joinedAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    public GroupMemberId getId() {
        return id;
    }

    public void setId(final GroupMemberId id) {
        this.id = id;
    }

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(final GroupEntity group) {
        this.group = group;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(final UserEntity user) {
        this.user = user;
    }

    public GroupMemberRole getRole() {
        return role;
    }

    public void setRole(final GroupMemberRole role) {
        this.role = role;
    }

    public GroupMemberStatus getStatus() {
        return status;
    }

    public void setStatus(final GroupMemberStatus status) {
        this.status = status;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(final OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
