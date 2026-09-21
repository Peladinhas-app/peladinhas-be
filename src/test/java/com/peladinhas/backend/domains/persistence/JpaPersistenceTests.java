package com.peladinhas.backend.domains.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionReason;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingStatus;
import com.peladinhas.backend.domains.chats.persistence.ChatEntity;
import com.peladinhas.backend.domains.chats.persistence.ChatMemberEntity;
import com.peladinhas.backend.domains.chats.persistence.ChatMemberId;
import com.peladinhas.backend.domains.chats.persistence.ChatMemberRepository;
import com.peladinhas.backend.domains.chats.persistence.ChatRepository;
import com.peladinhas.backend.domains.chats.persistence.ChatType;
import com.peladinhas.backend.domains.chats.persistence.MessageEntity;
import com.peladinhas.backend.domains.chats.persistence.MessageRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberId;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRole;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberStatus;
import com.peladinhas.backend.domains.groups.persistence.GroupRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminId;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantStatus;
import com.peladinhas.backend.domains.matches.persistence.MatchPriceAdjustmentEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchPriceAdjustmentRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchStatus;
import com.peladinhas.backend.domains.notifications.persistence.NotificationEntity;
import com.peladinhas.backend.domains.notifications.persistence.NotificationRepository;
import com.peladinhas.backend.domains.payments.persistence.PaymentEntity;
import com.peladinhas.backend.domains.payments.persistence.PaymentRepository;
import com.peladinhas.backend.domains.payments.persistence.PaymentStatus;
import com.peladinhas.backend.domains.payments.persistence.PaymentType;
import com.peladinhas.backend.domains.payments.persistence.RefundEntity;
import com.peladinhas.backend.domains.payments.persistence.RefundReason;
import com.peladinhas.backend.domains.payments.persistence.RefundRepository;
import com.peladinhas.backend.domains.payments.persistence.RefundStatus;
import com.peladinhas.backend.domains.pitches.persistence.PitchBlockEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchBlockRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchImageEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchImageRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleRepository;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.support.PostgreSqlContainerTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class JpaPersistenceTests extends PostgreSqlContainerTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private PitchImageRepository pitchImageRepository;

    @Autowired
    private PitchScheduleRepository pitchScheduleRepository;

    @Autowired
    private PitchBlockRepository pitchBlockRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchAdminRepository matchAdminRepository;

    @Autowired
    private MatchParticipantRepository matchParticipantRepository;

    @Autowired
    private MatchPriceAdjustmentRepository matchPriceAdjustmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingRejectionRepository bookingRejectionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatMemberRepository chatMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Verifies that representative persistence mappings save and reload.
     */
    @Test
    void persistsAndLoadsRepresentativeEntityGraph() {
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity user = saveUser(now);
        GroupEntity group = saveGroup(user, now);
        saveGroupMember(group, user, now);
        PitchEntity pitch = savePitch(user, now);
        savePitchAvailability(pitch, now);
        MatchEntity match = saveMatch(group, user, now);
        saveMatchAdmin(match, user, now);
        MatchParticipantEntity participant = saveParticipant(match, user, now);
        savePriceAdjustment(match, user, now);
        BookingEntity booking = saveBooking(match, pitch, now);
        saveBookingRejection(booking, now);
        PaymentEntity payment = savePayment(participant, now);
        RefundEntity refund = saveRefund(payment, now);
        ChatEntity chat = saveChat(group, now);
        saveChatMember(chat, user, now);
        MessageEntity message = saveMessage(chat, user, now);
        NotificationEntity notification = saveNotification(user, match.getId(), now);

        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(user.getId())).isPresent();
        assertThat(groupRepository.findById(group.getId())).isPresent();
        assertThat(pitchRepository.findById(pitch.getId())).isPresent();
        assertThat(matchRepository.findById(match.getId())).isPresent();
        assertThat(matchParticipantRepository.findById(participant.getId())).isPresent();
        assertThat(bookingRepository.findById(booking.getId())).isPresent();
        assertThat(paymentRepository.findById(payment.getId())).isPresent();
        assertThat(refundRepository.findById(refund.getId())).isPresent();
        assertThat(chatRepository.findById(chat.getId())).isPresent();
        assertThat(messageRepository.findById(message.getId())).isPresent();
        assertThat(notificationRepository.findById(notification.getId()))
                .get()
                .extracting(NotificationEntity::getPayload)
                .satisfies(payload -> assertThat(payload).containsEntry("matchId", match.getId().toString()));
    }

    private UserEntity saveUser(final OffsetDateTime now) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("jpa-%s@example.test".formatted(user.getId()));
        user.setName("JPA User");
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setAuthProvider("test");
        user.setAuthSubject(user.getId().toString());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private GroupEntity saveGroup(final UserEntity user, final OffsetDateTime now) {
        GroupEntity group = new GroupEntity();
        group.setId(UUID.randomUUID());
        group.setName("JPA Group");
        group.setVisibility(GroupVisibility.PUBLIC);
        group.setCreatedByUser(user);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        return groupRepository.save(group);
    }

    private void saveGroupMember(final GroupEntity group, final UserEntity user, final OffsetDateTime now) {
        GroupMemberEntity member = new GroupMemberEntity();
        member.setId(new GroupMemberId(group.getId(), user.getId()));
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMemberRole.ADMIN);
        member.setStatus(GroupMemberStatus.ACTIVE);
        member.setJoinedAt(now);
        member.setUpdatedAt(now);
        groupMemberRepository.save(member);
    }

    private PitchEntity savePitch(final UserEntity user, final OffsetDateTime now) {
        PitchEntity pitch = new PitchEntity();
        pitch.setId(UUID.randomUUID());
        pitch.setOwnerUser(user);
        pitch.setName("JPA Pitch");
        pitch.setAddress("Rua Teste 1");
        pitch.setLatitude(new BigDecimal("38.720000"));
        pitch.setLongitude(new BigDecimal("-9.140000"));
        pitch.setTimezone("Europe/Lisbon");
        pitch.setBasePrice(new BigDecimal("60.00"));
        pitch.setCurrency("EUR");
        pitch.setActive(true);
        pitch.setCreatedAt(now);
        pitch.setUpdatedAt(now);
        return pitchRepository.save(pitch);
    }

    private void savePitchAvailability(final PitchEntity pitch, final OffsetDateTime now) {
        PitchImageEntity image = new PitchImageEntity();
        image.setId(UUID.randomUUID());
        image.setPitch(pitch);
        image.setImageUrl("https://example.test/pitch.jpg");
        image.setDisplayOrder(0);
        image.setCreatedAt(now);
        pitchImageRepository.save(image);

        PitchScheduleEntity schedule = new PitchScheduleEntity();
        schedule.setId(UUID.randomUUID());
        schedule.setPitch(pitch);
        schedule.setDayOfWeek((short) 1);
        schedule.setStartsAt(LocalTime.of(18, 0));
        schedule.setEndsAt(LocalTime.of(23, 0));
        pitchScheduleRepository.save(schedule);

        PitchBlockEntity block = new PitchBlockEntity();
        block.setId(UUID.randomUUID());
        block.setPitch(pitch);
        block.setStartsAt(now.plusDays(20));
        block.setEndsAt(now.plusDays(20).plusHours(2));
        block.setReasonCode("maintenance");
        block.setCreatedAt(now);
        pitchBlockRepository.save(block);
    }

    private MatchEntity saveMatch(final GroupEntity group, final UserEntity user, final OffsetDateTime now) {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setGroup(group);
        match.setCreatedByUser(user);
        match.setStartsAt(now.plusDays(10));
        match.setEndsAt(now.plusDays(10).plusMinutes(90));
        match.setMaxPlayers(10);
        match.setJoinMode(MatchJoinMode.OPEN_JOIN);
        match.setStatus(MatchStatus.DRAFT);
        match.setPublicVacanciesEnabled(true);
        match.setCreatedAt(now);
        match.setUpdatedAt(now);
        return matchRepository.save(match);
    }

    private void saveMatchAdmin(final MatchEntity match, final UserEntity user, final OffsetDateTime now) {
        MatchAdminEntity admin = new MatchAdminEntity();
        admin.setId(new MatchAdminId(match.getId(), user.getId()));
        admin.setMatch(match);
        admin.setUser(user);
        admin.setAssignedAt(now);
        matchAdminRepository.save(admin);
    }

    private MatchParticipantEntity saveParticipant(
            final MatchEntity match,
            final UserEntity user,
            final OffsetDateTime now) {
        MatchParticipantEntity participant = new MatchParticipantEntity();
        participant.setId(UUID.randomUUID());
        participant.setMatch(match);
        participant.setUser(user);
        participant.setStatus(MatchParticipantStatus.CONFIRMED);
        participant.setJoinedAt(now);
        participant.setConfirmedAt(now);
        return matchParticipantRepository.save(participant);
    }

    private void savePriceAdjustment(final MatchEntity match, final UserEntity user, final OffsetDateTime now) {
        MatchPriceAdjustmentEntity adjustment = new MatchPriceAdjustmentEntity();
        adjustment.setId(UUID.randomUUID());
        adjustment.setMatch(match);
        adjustment.setOldPlayerCount(12);
        adjustment.setNewPlayerCount(10);
        adjustment.setOldPricePerPlayer(new BigDecimal("5.00"));
        adjustment.setNewPricePerPlayer(new BigDecimal("6.00"));
        adjustment.setCurrency("EUR");
        adjustment.setCreatedByUser(user);
        adjustment.setCreatedAt(now);
        matchPriceAdjustmentRepository.save(adjustment);
    }

    private BookingEntity saveBooking(final MatchEntity match, final PitchEntity pitch, final OffsetDateTime now) {
        BookingEntity booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setMatch(match);
        booking.setPitch(pitch);
        booking.setStartsAt(match.getStartsAt());
        booking.setEndsAt(match.getEndsAt());
        booking.setTotalPrice(new BigDecimal("60.00"));
        booking.setCurrency("EUR");
        booking.setStatus(BookingStatus.PROVISIONAL);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        return bookingRepository.save(booking);
    }

    private void saveBookingRejection(final BookingEntity booking, final OffsetDateTime now) {
        BookingRejectionEntity rejection = new BookingRejectionEntity();
        rejection.setId(UUID.randomUUID());
        rejection.setBooking(booking);
        rejection.setReasonCode(BookingRejectionReason.OTHER);
        rejection.setExplanation("Persistence test rejection history");
        rejection.setCreatedAt(now);
        bookingRejectionRepository.save(rejection);
    }

    private PaymentEntity savePayment(final MatchParticipantEntity participant, final OffsetDateTime now) {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        payment.setParticipant(participant);
        payment.setType(PaymentType.INITIAL);
        payment.setAmount(new BigDecimal("10.00"));
        payment.setServiceFee(new BigDecimal("1.00"));
        payment.setCurrency("EUR");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setProvider("test_provider");
        payment.setProviderPaymentId("payment-%s".formatted(payment.getId()));
        payment.setCreatedAt(now);
        payment.setPaidAt(now);
        return paymentRepository.save(payment);
    }

    private RefundEntity saveRefund(final PaymentEntity payment, final OffsetDateTime now) {
        RefundEntity refund = new RefundEntity();
        refund.setId(UUID.randomUUID());
        refund.setPayment(payment);
        refund.setAmount(new BigDecimal("5.00"));
        refund.setReasonCode(RefundReason.BOOKING_LOST);
        refund.setStatus(RefundStatus.PENDING);
        refund.setProviderRefundId("refund-%s".formatted(refund.getId()));
        refund.setCreatedAt(now);
        return refundRepository.save(refund);
    }

    private ChatEntity saveChat(final GroupEntity group, final OffsetDateTime now) {
        ChatEntity chat = new ChatEntity();
        chat.setId(UUID.randomUUID());
        chat.setType(ChatType.GROUP);
        chat.setGroup(group);
        chat.setCreatedAt(now);
        return chatRepository.save(chat);
    }

    private void saveChatMember(final ChatEntity chat, final UserEntity user, final OffsetDateTime now) {
        ChatMemberEntity chatMember = new ChatMemberEntity();
        chatMember.setId(new ChatMemberId(chat.getId(), user.getId()));
        chatMember.setChat(chat);
        chatMember.setUser(user);
        chatMember.setJoinedAt(now);
        chatMemberRepository.save(chatMember);
    }

    private MessageEntity saveMessage(final ChatEntity chat, final UserEntity user, final OffsetDateTime now) {
        MessageEntity message = new MessageEntity();
        message.setId(UUID.randomUUID());
        message.setChat(chat);
        message.setSenderUser(user);
        message.setContent("Persistence test message");
        message.setCreatedAt(now);
        return messageRepository.save(message);
    }

    private NotificationEntity saveNotification(
            final UserEntity user,
            final UUID matchId,
            final OffsetDateTime now) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setUser(user);
        notification.setType("booking_confirmed");
        notification.setRelatedEntityType("match");
        notification.setRelatedEntityId(matchId);
        notification.setPayload(Map.of("matchId", matchId.toString()));
        notification.setCreatedAt(now);
        return notificationRepository.save(notification);
    }
}
