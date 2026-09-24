package com.peladinhas.backend.domains.matches.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "matches")
public class MatchEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdByUser;

    @Column(name = "starts_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime endsAt;

    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers;

    @Convert(converter = MatchJoinMode.ConverterImpl.class)
    @Column(name = "join_mode", nullable = false, columnDefinition = "varchar")
    private MatchJoinMode joinMode;

    @Convert(converter = MatchStatus.ConverterImpl.class)
    @Column(name = "status", nullable = false, columnDefinition = "varchar")
    private MatchStatus status;

    @Convert(converter = MatchFundingMode.ConverterImpl.class)
    @Column(name = "funding_mode", nullable = false, columnDefinition = "varchar")
    private MatchFundingMode fundingMode;

    @Convert(converter = MatchFundingState.ConverterImpl.class)
    @Column(name = "funding_state", nullable = false, columnDefinition = "varchar")
    private MatchFundingState fundingState;

    @Column(name = "public_vacancies_enabled", nullable = false)
    private Boolean publicVacanciesEnabled;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(final GroupEntity group) {
        this.group = group;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(final UserEntity createdByUser) {
        this.createdByUser = createdByUser;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(final OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(final OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(final Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public MatchJoinMode getJoinMode() {
        return joinMode;
    }

    public void setJoinMode(final MatchJoinMode joinMode) {
        this.joinMode = joinMode;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(final MatchStatus status) {
        this.status = status;
    }

    public MatchFundingMode getFundingMode() {
        return fundingMode;
    }

    public void setFundingMode(final MatchFundingMode fundingMode) {
        this.fundingMode = fundingMode;
    }

    public MatchFundingState getFundingState() {
        return fundingState;
    }

    public void setFundingState(final MatchFundingState fundingState) {
        this.fundingState = fundingState;
    }

    public Boolean getPublicVacanciesEnabled() {
        return publicVacanciesEnabled;
    }

    public void setPublicVacanciesEnabled(final Boolean publicVacanciesEnabled) {
        this.publicVacanciesEnabled = publicVacanciesEnabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
