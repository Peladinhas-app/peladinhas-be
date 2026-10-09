package com.peladinhas.backend.domains.matches.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.service.MatchSummaryRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchSummaryRepository {

    private static final String CAPACITY_STATUS_FILTER = "'approved', 'awaiting_payment', 'confirmed'";
    private static final String UPCOMING_PARTICIPANT_STATUS_FILTER =
            "'requested', 'approved', 'awaiting_payment', 'confirmed'";
    private static final RowMapper<MatchSummaryRow> ROW_MAPPER = new MatchSummaryRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MatchSummaryRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MatchSummaryRow> findUpcomingForViewer(final UUID userId, final OffsetDateTime now) {
        String sql = """
                select
                    m.id as match_id,
                    g.name as display_name,
                    m.starts_at,
                    m.ends_at,
                    m.max_players,
                    (
                        select count(*)
                        from match_participants capacity_participant
                        where capacity_participant.match_id = m.id
                          and capacity_participant.status in (%s)
                    ) as occupied_places,
                    m.status as match_status,
                    m.join_mode,
                    exists (
                        select 1
                        from match_admins viewer_admin
                        where viewer_admin.match_id = m.id
                          and viewer_admin.user_id = :userId
                    ) as viewer_is_organizer,
                    viewer_participant.status as viewer_participation_status
                from matches m
                join groups g on g.id = m.group_id
                left join match_participants viewer_participant
                    on viewer_participant.match_id = m.id
                   and viewer_participant.user_id = :userId
                where m.ends_at > :now
                  and m.status in ('draft', 'recruiting', 'ready')
                  and (
                      exists (
                          select 1
                          from match_admins viewer_admin
                          where viewer_admin.match_id = m.id
                            and viewer_admin.user_id = :userId
                      )
                      or viewer_participant.status in (%s)
                  )
                order by m.starts_at asc, m.id asc
                """.formatted(CAPACITY_STATUS_FILTER, UPCOMING_PARTICIPANT_STATUS_FILTER);
        return jdbcTemplate.query(sql, parameters(userId, now), ROW_MAPPER);
    }

    private MapSqlParameterSource parameters(final UUID userId, final OffsetDateTime now) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("now", now);
    }

    private static final class MatchSummaryRowMapper implements RowMapper<MatchSummaryRow> {

        @Override
        public MatchSummaryRow mapRow(final ResultSet resultSet, final int rowNumber) throws SQLException {
            return new MatchSummaryRow(
                    resultSet.getObject("match_id", UUID.class),
                    resultSet.getString("display_name"),
                    offsetDateTime(resultSet, "starts_at"),
                    offsetDateTime(resultSet, "ends_at"),
                    resultSet.getInt("max_players"),
                    resultSet.getLong("occupied_places"),
                    resultSet.getString("match_status"),
                    resultSet.getString("join_mode"),
                    resultSet.getBoolean("viewer_is_organizer"),
                    resultSet.getString("viewer_participation_status"));
        }

        private OffsetDateTime offsetDateTime(final ResultSet resultSet, final String columnName) throws SQLException {
            Timestamp timestamp = resultSet.getTimestamp(columnName);
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
    }
}
