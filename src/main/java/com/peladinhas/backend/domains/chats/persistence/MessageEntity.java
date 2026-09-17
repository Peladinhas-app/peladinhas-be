package com.peladinhas.backend.domains.chats.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages")
public class MessageEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private UserEntity senderUser;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "edited_at", columnDefinition = "timestamptz")
    private OffsetDateTime editedAt;

    public ChatEntity getChat() {
        return chat;
    }

    public void setChat(final ChatEntity chat) {
        this.chat = chat;
    }

    public UserEntity getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(final UserEntity senderUser) {
        this.senderUser = senderUser;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(final OffsetDateTime editedAt) {
        this.editedAt = editedAt;
    }
}
