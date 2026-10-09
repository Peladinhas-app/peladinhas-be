package com.peladinhas.backend.domains.matches.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.service.MatchDiscoveryCriteria;
import com.peladinhas.backend.domains.matches.service.MatchDiscoveryRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchDiscoveryRepository {

    private static final String CAPACITY_STATUS_FILTER = "'approved', 'awaiting_payment', 'confirmed'";
    private static final String BOOKING_STATUS_FILTER = "'provisional', 'confirmed'";
    private static final RowMapper<MatchDiscoveryRow> ROW_MAPPER = new MatchDiscoveryRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MatchDiscoveryRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MatchDiscoveryRow> findDiscoverableMatches(final MatchDiscoveryCriteria criteria) {
        String sql = """
                select
                    m.id as match_id,
                    g.name as display_name,
                    m.starts_at,
                    m.ends_at,
                    m.max_players,
                    (
                        select count(*)
                        from match_participants mp
                        where mp.match_id = m.id
                          and mp.status in (%s)
                    ) as occupied_places,
                    b.pitch_id,
                    p.name as pitch_name,
                    p.address as pitch_address,
                    p.base_price as pitch_base_price,
                    p.currency as pitch_currency,
                    g.visibility as group_visibility,
                    m.join_mode,
                    exists (
                        select 1
                        from match_admins viewer_admin
                        where viewer_admin.match_id = m.id
                          and viewer_admin.user_id = :userId
                    ) as viewer_is_organizer,
                    viewer_participant.status as viewer_participation_status
                %s
                where %s
                order by m.starts_at asc, m.id asc
                limit :size offset :offset
                """.formatted(CAPACITY_STATUS_FILTER, fromClause(), whereClause());
        return jdbcTemplate.query(sql, parameters(criteria), ROW_MAPPER);
    }

    public long countDiscoverableMatches(final MatchDiscoveryCriteria criteria) {
        String sql = """
                select count(*)
                %s
                where %s
                """.formatted(fromClause(), whereClause());
        Long count = jdbcTemplate.queryForObject(sql, parameters(criteria), Long.class);
        return count == null ? 0L : count;
    }

    private String fromClause() {
        return """
                from matches m
                join groups g on g.id = m.group_id
                left join lateral (
                    select booking.*
                    from bookings booking
                    where booking.match_id = m.id
                      and booking.status in (%s)
                    order by
                        case booking.status
                            when 'confirmed' then 0
                            when 'provisional' then 1
                            else 2
                        end asc,
                        case
                            when booking.status = 'confirmed' then booking.confirmed_at
                            else booking.created_at
                        end desc,
                        booking.created_at desc,
                        booking.id asc
                    limit 1
                ) b on true
                left join pitches p on p.id = b.pitch_id
                left join match_participants viewer_participant
                    on viewer_participant.match_id = m.id
                   and viewer_participant.user_id = :userId
                """.formatted(BOOKING_STATUS_FILTER);
    }

    private String whereClause() {
        return """
                m.status = 'recruiting'
                and m.starts_at > :now
                and (:hasStartsFrom = false or m.starts_at >= :startsFrom)
                and (:hasStartsTo = false or m.starts_at <= :startsTo)
                and (:hasTimeFrom = false or cast((m.starts_at at time zone coalesce(p.timezone, cast(:fallbackTimeZone as varchar))) as time) >= :timeFrom)
                and (:hasTimeTo = false or cast((m.starts_at at time zone coalesce(p.timezone, cast(:fallbackTimeZone as varchar))) as time) <= :timeTo)
                and (:hasJoinMode = false or m.join_mode = :joinMode)
                and (
                    g.visibility = 'public'
                    or m.public_vacancies_enabled = true
                    or exists (
                        select 1
                        from group_members gm
                        where gm.group_id = m.group_id
                          and gm.user_id = :userId
                          and gm.status = 'active'
                    )
                )
                and (
                    :hasAreaPattern = false
                    or lower(g.name) like :areaPattern
                    or lower(p.name) like :areaPattern
                    or lower(p.address) like :areaPattern
                )
                and (
                    :availableOnly = false
                    or m.max_players > (
                        select count(*)
                        from match_participants capacity_participant
                        where capacity_participant.match_id = m.id
                          and capacity_participant.status in (%s)
                    )
                )
                """.formatted(CAPACITY_STATUS_FILTER);
    }

    private MapSqlParameterSource parameters(final MatchDiscoveryCriteria criteria) {
        int offset = criteria.page() * criteria.size();
        return new MapSqlParameterSource()
                .addValue("userId", criteria.userId())
                .addValue("now", criteria.now())
                .addValue("startsFrom", criteria.startsFrom())
                .addValue("hasStartsFrom", criteria.startsFrom() != null)
                .addValue("startsTo", criteria.startsTo())
                .addValue("hasStartsTo", criteria.startsTo() != null)
                .addValue("timeFrom", criteria.timeFrom())
                .addValue("hasTimeFrom", criteria.timeFrom() != null)
                .addValue("timeTo", criteria.timeTo())
                .addValue("hasTimeTo", criteria.timeTo() != null)
                .addValue("joinMode", criteria.joinMode() == null ? null : criteria.joinMode().value())
                .addValue("hasJoinMode", criteria.joinMode() != null)
                .addValue("areaPattern", areaPattern(criteria.area()))
                .addValue("hasAreaPattern", areaPattern(criteria.area()) != null)
                .addValue("fallbackTimeZone", criteria.fallbackTimeZone())
                .addValue("availableOnly", criteria.availableOnly())
                .addValue("size", criteria.size())
                .addValue("offset", offset);
    }

    private String areaPattern(final String area) {
        if (area == null || area.isBlank()) {
            return null;
        }
        return "%" + area.trim().toLowerCase() + "%";
    }

    private static final class MatchDiscoveryRowMapper implements RowMapper<MatchDiscoveryRow> {

        @Override
        public MatchDiscoveryRow mapRow(final ResultSet resultSet, final int rowNumber) throws SQLException {
            return new MatchDiscoveryRow(
                    resultSet.getObject("match_id", UUID.class),
                    resultSet.getString("display_name"),
                    offsetDateTime(resultSet, "starts_at"),
                    offsetDateTime(resultSet, "ends_at"),
                    resultSet.getInt("max_players"),
                    resultSet.getLong("occupied_places"),
                    nullableUuid(resultSet, "pitch_id"),
                    resultSet.getString("pitch_name"),
                    resultSet.getString("pitch_address"),
                    resultSet.getBigDecimal("pitch_base_price"),
                    resultSet.getString("pitch_currency"),
                    resultSet.getString("group_visibility"),
                    resultSet.getString("join_mode"),
                    resultSet.getBoolean("viewer_is_organizer"),
                    resultSet.getString("viewer_participation_status"));
        }

        private UUID nullableUuid(final ResultSet resultSet, final String columnName) throws SQLException {
            Object value = resultSet.getObject(columnName);
            return value == null ? null : resultSet.getObject(columnName, UUID.class);
        }

        private OffsetDateTime offsetDateTime(final ResultSet resultSet, final String columnName) throws SQLException {
            Timestamp timestamp = resultSet.getTimestamp(columnName);
            return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
    }
}
