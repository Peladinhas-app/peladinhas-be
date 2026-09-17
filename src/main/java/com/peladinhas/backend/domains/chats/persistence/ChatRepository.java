package com.peladinhas.backend.domains.chats.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<ChatEntity, UUID> {
}
