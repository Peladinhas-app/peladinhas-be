package com.peladinhas.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.peladinhas.backend.support.PostgreSqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseConnectionTests extends PostgreSqlContainerTest {

    private static final List<String> EXPECTED_APPLICATION_TABLES = List.of(
            "booking_rejections",
            "bookings",
            "chat_members",
            "chats",
            "group_members",
            "groups",
            "match_admins",
            "match_participants",
            "match_price_adjustments",
            "matches",
            "messages",
            "notifications",
            "payments",
            "pitch_blocks",
            "pitch_images",
            "pitch_schedules",
            "pitches",
            "refunds",
            "users");

    private final JdbcTemplate jdbcTemplate;

    DatabaseConnectionTests(@Autowired final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verifies that Spring can reach the temporary PostgreSQL database.
     */
    @Test
    void connectsToTemporaryPostgreSQL() {
        Map<String, Object> result = jdbcTemplate.queryForMap(
                "select current_database() as database_name, current_user as user_name");

        assertThat(result)
                .containsEntry("database_name", "peladinhas_test")
                .containsEntry("user_name", "peladinhas_test");
    }

    /**
     * Verifies that Flyway builds the initial schema from an empty database.
     */
    @Test
    void appliesInitialSchemaMigration() {
        List<String> applicationTables = applicationTables();
        Integer successfulMigrations = jdbcTemplate.queryForObject("""
                select count(*)
                from flyway_schema_history
                where success = true
                """, Integer.class);

        assertThat(applicationTables).containsExactlyElementsOf(EXPECTED_APPLICATION_TABLES);
        assertThat(successfulMigrations).isEqualTo(1);
    }

    /**
     * Verifies that account email uniqueness is enforced.
     */
    @Test
    void rejectsDuplicateUserEmail() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        String email = "duplicate-%s@example.test".formatted(UUID.randomUUID());

        insertUser(firstUserId, email);

        assertThatThrownBy(() -> insertUser(secondUserId, email))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Verifies that group membership is unique per user and group.
     */
    @Test
    void rejectsDuplicateGroupMembership() {
        GraphIds graph = insertGraph();

        insertGroupMember(graph.groupId(), graph.ownerUserId());

        assertThatThrownBy(() -> insertGroupMember(graph.groupId(), graph.ownerUserId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Verifies that match participation is unique per user and match.
     */
    @Test
    void rejectsDuplicateMatchParticipant() {
        GraphIds graph = insertGraph();

        insertMatchParticipant(graph.participantId(), graph.matchId(), graph.ownerUserId(), "requested");

        assertThatThrownBy(() -> insertMatchParticipant(UUID.randomUUID(), graph.matchId(), graph.ownerUserId(), "requested"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Verifies that matches cannot end before they start.
     */
    @Test
    void rejectsInvalidMatchTimeRange() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        insertUser(userId, "match-time-%s@example.test".formatted(userId));
        insertGroup(groupId, userId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into matches (
                    id, group_id, created_by_user_id, starts_at, ends_at, max_players,
                    join_mode, status, public_vacancies_enabled, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, 10, 'open_join', 'draft', true, ?, ?)
                """, UUID.randomUUID(), groupId, userId, now.plusHours(2), now.plusHours(1), now, now))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Verifies that pitch coordinates must be valid and paired.
     */
    @Test
    void rejectsInvalidPitchCoordinates() {
        UUID userId = UUID.randomUUID();

        insertUser(userId, "pitch-coordinates-%s@example.test".formatted(userId));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into pitches (
                    id, owner_user_id, name, address, latitude, longitude, timezone,
                    base_price, currency, is_active, created_at, updated_at
                )
                values (?, ?, 'North Field', 'Street 1', 95, null, 'Europe/Lisbon', 60.00, 'EUR', true, ?, ?)
                """, UUID.randomUUID(), userId, OffsetDateTime.now(), OffsetDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Verifies that each chat has exactly one matching context.
     */
    @Test
    void rejectsInvalidChatContextCombination() {
        GraphIds graph = insertGraph();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into chats (id, type, group_id, match_id, booking_id, created_at)
                values (?, 'group', ?, ?, null, ?)
                """, UUID.randomUUID(), graph.groupId(), graph.matchId(), OffsetDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Verifies that status columns accept only approved English values.
     */
    @Test
    void rejectsInvalidStatusValue() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        insertUser(userId, "invalid-status-%s@example.test".formatted(userId));
        insertGroup(groupId, userId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into matches (
                    id, group_id, created_by_user_id, starts_at, ends_at, max_players,
                    join_mode, status, public_vacancies_enabled, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, 10, 'open_join', 'invented_status', true, ?, ?)
                """, UUID.randomUUID(), groupId, userId, now.plusHours(1), now.plusHours(2), now, now))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Verifies that refund provider references are not globally unique yet.
     */
    @Test
    void allowsDuplicateRefundProviderReferenceUntilProviderStrategyIsFinalized() {
        GraphIds graph = insertGraph();
        UUID paymentId = UUID.randomUUID();
        String providerRefundId = "refund-%s".formatted(UUID.randomUUID());

        insertMatchParticipant(graph.participantId(), graph.matchId(), graph.ownerUserId(), "confirmed");
        insertPayment(paymentId, graph.participantId());
        insertRefund(UUID.randomUUID(), paymentId, providerRefundId);
        insertRefund(UUID.randomUUID(), paymentId, providerRefundId);

        Integer refundCount = jdbcTemplate.queryForObject("""
                select count(*)
                from refunds
                where provider_refund_id = ?
                """, Integer.class, providerRefundId);

        assertThat(refundCount).isEqualTo(2);
    }

    /**
     * Reads application tables without Flyway metadata.
     *
     * @return sorted table names created by Peladinhas migrations
     */
    private List<String> applicationTables() {
        return jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                    and table_name <> 'flyway_schema_history'
                order by table_name
                """, String.class);
    }

    /**
     * Creates the minimum related rows needed by relationship tests.
     *
     * @return identifiers for the created records
     */
    private GraphIds insertGraph() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID pitchId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        insertUser(userId, "graph-%s@example.test".formatted(userId));
        insertGroup(groupId, userId);
        insertPitch(pitchId, userId);
        insertMatch(matchId, groupId, userId);
        insertBooking(bookingId, matchId, pitchId);

        return new GraphIds(userId, groupId, pitchId, matchId, bookingId, participantId);
    }

    /**
     * Adds a user row for constraint tests.
     *
     * @param userId user identifier
     * @param email unique email to store
     */
    private void insertUser(final UUID userId, final String email) {
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                insert into users (id, email, name, preferred_language, created_at, updated_at)
                values (?, ?, 'Test User', 'en', ?, ?)
                """, userId, email, now, now);
    }

    /**
     * Adds a group row for relationship tests.
     *
     * @param groupId group identifier
     * @param createdByUserId creator user identifier
     */
    private void insertGroup(final UUID groupId, final UUID createdByUserId) {
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                insert into groups (id, name, visibility, created_by_user_id, created_at, updated_at)
                values (?, 'Test Group', 'public', ?, ?, ?)
                """, groupId, createdByUserId, now, now);
    }

    /**
     * Adds a group member row for uniqueness tests.
     *
     * @param groupId group identifier
     * @param userId user identifier
     */
    private void insertGroupMember(final UUID groupId, final UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                insert into group_members (group_id, user_id, role, status, joined_at, updated_at)
                values (?, ?, 'admin', 'active', ?, ?)
                """, groupId, userId, now, now);
    }

    /**
     * Adds a pitch row for booking relationship tests.
     *
     * @param pitchId pitch identifier
     * @param ownerUserId owner user identifier
     */
    private void insertPitch(final UUID pitchId, final UUID ownerUserId) {
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                insert into pitches (
                    id, owner_user_id, name, address, latitude, longitude, timezone,
                    base_price, currency, is_active, created_at, updated_at
                )
                values (?, ?, 'Test Pitch', 'Street 1', 38.72, -9.14, 'Europe/Lisbon', 60.00, 'EUR', true, ?, ?)
                """, pitchId, ownerUserId, now, now);
    }

    /**
     * Adds a match row for relationship tests.
     *
     * @param matchId match identifier
     * @param groupId group identifier
     * @param createdByUserId creator user identifier
     */
    private void insertMatch(final UUID matchId, final UUID groupId, final UUID createdByUserId) {
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                insert into matches (
                    id, group_id, created_by_user_id, starts_at, ends_at, max_players,
                    join_mode, status, public_vacancies_enabled, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, 10, 'open_join', 'draft', true, ?, ?)
                """, matchId, groupId, createdByUserId, now.plusDays(1), now.plusDays(1).plusHours(1), now, now);
    }

    /**
     * Adds a booking row for chat context tests.
     *
     * @param bookingId booking identifier
     * @param matchId match identifier
     * @param pitchId pitch identifier
     */
    private void insertBooking(final UUID bookingId, final UUID matchId, final UUID pitchId) {
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                insert into bookings (
                    id, match_id, pitch_id, starts_at, ends_at, total_price, currency,
                    status, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, 60.00, 'EUR', 'provisional', ?, ?)
                """, bookingId, matchId, pitchId, now.plusDays(1), now.plusDays(1).plusHours(1), now, now);
    }

    /**
     * Adds a participant row for uniqueness tests.
     *
     * @param participantId participant identifier
     * @param matchId match identifier
     * @param userId user identifier
     * @param status approved participant status
     */
    private void insertMatchParticipant(
            final UUID participantId,
            final UUID matchId,
            final UUID userId,
            final String status) {
        jdbcTemplate.update("""
                insert into match_participants (id, match_id, user_id, status, joined_at)
                values (?, ?, ?, ?, ?)
                """, participantId, matchId, userId, status, OffsetDateTime.now());
    }

    /**
     * Adds a payment row for refund tests.
     *
     * @param paymentId payment identifier
     * @param participantId participant identifier
     */
    private void insertPayment(final UUID paymentId, final UUID participantId) {
        jdbcTemplate.update("""
                insert into payments (
                    id, participant_id, type, amount, service_fee, currency,
                    status, provider, provider_payment_id, created_at
                )
                values (?, ?, 'initial', 10.00, 1.00, 'EUR', 'succeeded', 'test_provider', ?, ?)
                """, paymentId, participantId, "payment-%s".formatted(paymentId), OffsetDateTime.now());
    }

    /**
     * Adds a refund row for provider-reference tests.
     *
     * @param refundId refund identifier
     * @param paymentId payment identifier
     * @param providerRefundId provider-side refund reference
     */
    private void insertRefund(final UUID refundId, final UUID paymentId, final String providerRefundId) {
        jdbcTemplate.update("""
                insert into refunds (
                    id, payment_id, amount, reason_code, status,
                    provider_refund_id, created_at
                )
                values (?, ?, 5.00, 'booking_lost', 'pending', ?, ?)
                """, refundId, paymentId, providerRefundId, OffsetDateTime.now());
    }

    private record GraphIds(
            UUID ownerUserId,
            UUID groupId,
            UUID pitchId,
            UUID matchId,
            UUID bookingId,
            UUID participantId) {
    }
}
