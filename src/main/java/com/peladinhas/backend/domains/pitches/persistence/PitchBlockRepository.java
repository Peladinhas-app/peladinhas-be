package com.peladinhas.backend.domains.pitches.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PitchBlockRepository extends JpaRepository<PitchBlockEntity, UUID> {

    @Query("""
            select count(block) > 0
            from PitchBlockEntity block
            where block.pitch.id = :pitchId
              and block.startsAt < :endsAt
              and block.endsAt > :startsAt
            """)
    boolean existsOverlappingBlock(
            @Param("pitchId") UUID pitchId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);
}
