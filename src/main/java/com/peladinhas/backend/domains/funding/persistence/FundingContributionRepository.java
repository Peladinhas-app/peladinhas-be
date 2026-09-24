package com.peladinhas.backend.domains.funding.persistence;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingContributionRepository extends JpaRepository<FundingContributionEntity, UUID> {

    @Query("""
            select coalesce(sum(contribution.amount), 0)
            from FundingContributionEntity contribution
            where contribution.match.id = :matchId
              and contribution.state = com.peladinhas.backend.domains.funding.persistence.FundingContributionState.SETTLED
              and contribution.purpose in :coveringPurposes
              and contribution.currency = :currency
            """)
    BigDecimal sumSettledCoverageForMatch(
            @Param("matchId") UUID matchId,
            @Param("coveringPurposes") Collection<FundingContributionPurpose> coveringPurposes,
            @Param("currency") String currency);
}
