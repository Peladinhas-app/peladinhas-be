package com.peladinhas.backend.domains.chats.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMemberEntity, ChatMemberId> {
}
