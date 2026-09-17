package com.peladinhas.backend.domains.chats.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_members")
public class ChatMemberEntity {

    @EmbeddedId
    private ChatMemberId id = new ChatMemberId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "joined_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime joinedAt;

    public ChatMemberId getId() {
        return id;
    }

    public void setId(final ChatMemberId id) {
        this.id = id;
    }

    public ChatEntity getChat() {
        return chat;
    }

    public void setChat(final ChatEntity chat) {
        this.chat = chat;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(final UserEntity user) {
        this.user = user;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(final OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
