package com.busgo.service;

import com.busgo.entity.*;
import com.busgo.repository.BookingRepository;
import com.busgo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JourneyReminderServiceTest {

    @Mock
    private BookingRepository bookingRepo;

    @Mock
    private NotificationRepository notificationRepo;

    @InjectMocks
    private JourneyReminderService reminderService;

    private User testCustomer;
    private Bus testBus;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testCustomer = new User("Rahul Kumar", "rahul@example.com", "9876543210", "password", Role.CUSTOMER);
        testCustomer.setId(101L);

        testBus = new Bus();
        testBus.setId(1L);
        testBus.setName("Express Travels");
        testBus.setSource("Chennai");
        testBus.setDestination("Bangalore");
        // Bus departing in 1 hour (within 2-hour window)
        testBus.setDepartureTime(LocalDateTime.now().plusHours(1));
        testBus.setDurationMinutes(300);
        testBus.setTotalSeats(40);
        testBus.setFare(750.0);
        testBus.setStatus(BusStatus.ACTIVE);

        testBooking = new Booking();
        testBooking.setId(50L);
        testBooking.setBookingCode("BGTEST1234");
        testBooking.setCustomer(testCustomer);
        testBooking.setBus(testBus);
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setJourneyReminderSent(false);

        BookingSeat seat = new BookingSeat(testBooking, testBus, 12, "Rahul Kumar", "Male", "General Passenger", false);
        testBooking.setSeats(List.of(seat));
    }

    @Test
    void test1_CheckAndSendReminder_WhenWithin2Hours_SendsNotification() {
        boolean sent = reminderService.checkAndSendReminderForBooking(testBooking);

        assertTrue(sent);
        assertTrue(testBooking.isJourneyReminderSent());

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo, times(1)).save(notifCaptor.capture());
        verify(bookingRepo, times(1)).saveAndFlush(testBooking);

        Notification captured = notifCaptor.getValue();
        assertEquals("Your journey starts soon! Have a safe journey.", captured.getMessage());
        assertEquals("JOURNEY_REMINDER", captured.getType());
        assertTrue(captured.getDetails().contains("Chennai → Bangalore"));
        assertTrue(captured.getDetails().contains("Departure:"));
        assertTrue(captured.getDetails().contains("Seat: 12"));
    }

    @Test
    void test2_CheckAndSendReminder_WhenDepartureFarInFuture_DoesNotSend() {
        // Departure in 24 hours
        testBus.setDepartureTime(LocalDateTime.now().plusDays(1));

        boolean sent = reminderService.checkAndSendReminderForBooking(testBooking);

        assertFalse(sent);
        assertFalse(testBooking.isJourneyReminderSent());
        verify(notificationRepo, never()).save(any());
        verify(bookingRepo, never()).saveAndFlush(any());
    }

    @Test
    void test3_CheckAndSendReminder_WhenAlreadySent_DoesNotDuplicate() {
        testBooking.setJourneyReminderSent(true);

        boolean sent = reminderService.checkAndSendReminderForBooking(testBooking);

        assertFalse(sent);
        verify(notificationRepo, never()).save(any());
        verify(bookingRepo, never()).saveAndFlush(any());
    }

    @Test
    void test4_ProcessPendingReminders_ScheduledBatch() {
        when(bookingRepo.findPendingReminders(any(), any())).thenReturn(List.of(testBooking));

        int count = reminderService.processPendingReminders();

        assertEquals(1, count);
        assertTrue(testBooking.isJourneyReminderSent());
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }
}

