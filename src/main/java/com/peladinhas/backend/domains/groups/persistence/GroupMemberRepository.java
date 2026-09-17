package com.peladinhas.backend.domains.groups.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {
}
