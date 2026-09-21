package com.peladinhas.backend.domains.pitches.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PitchScheduleRepository extends JpaRepository<PitchScheduleEntity, UUID> {

    List<PitchScheduleEntity> findByPitch_IdAndDayOfWeek(UUID pitchId, Short dayOfWeek);
}
