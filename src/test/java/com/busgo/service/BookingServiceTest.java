package com.busgo.service;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.*;
import com.busgo.exception.ApiException;
import com.busgo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BusRepository busRepo;
    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private BookingSeatRepository seatRepo;
    @Mock
    private NotificationRepository notificationRepo;
    @Mock
    private FeedbackRepository feedbackRepo;

    @InjectMocks
    private BookingService bookingService;

    private User testCustomer;
    private Bus testBus;

    @BeforeEach
    void setUp() {
        testCustomer = new User("John Doe", "john@example.com", "9876543210", "password", Role.CUSTOMER);
        testCustomer.setId(10L);

        testBus = new Bus();
        testBus.setId(1L);
        testBus.setName("Express Bus");
        testBus.setSource("City A");
        testBus.setDestination("City B");
        testBus.setDepartureTime(LocalDateTime.now().plusDays(1));
        testBus.setTotalSeats(40);
        testBus.setFare(500.0);
        testBus.setStatus(BusStatus.ACTIVE);
    }

    @Test
    void test1_GeneralBooking_SingleSeat_Success() {
        when(busRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(testBus));
        when(seatRepo.findConfirmedSeatsByBusId(1L)).thenReturn(new ArrayList<>());
        when(bookingRepo.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PassengerRequest> passengers = List.of(
            new PassengerRequest(1, "John Doe", "Male", "Individual", false)
        );
        BookingRequest req = new BookingRequest(1L, null, passengers);

        Booking booking = bookingService.create(testCustomer, req);

        assertNotNull(booking);
        assertEquals(1, booking.getSeats().size());
        assertEquals("John Doe", booking.getSeats().get(0).getPassengerName());
        assertEquals("Male", booking.getSeats().get(0).getPassengerGender());
        assertEquals("Individual", booking.getSeats().get(0).getPassengerType());
        assertFalse(booking.getSeats().get(0).isWomenPreference());
    }

    @Test
    void test2_FamilyMultipleBooking_Success() {
        when(busRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(testBus));
        when(seatRepo.findConfirmedSeatsByBusId(1L)).thenReturn(new ArrayList<>());
        when(bookingRepo.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PassengerRequest> passengers = List.of(
            new PassengerRequest(1, "John Doe", "Male", "Family", false),
            new PassengerRequest(2, "Jane Doe", "Female", "Family", false),
            new PassengerRequest(3, "Kid Doe", "Male", "Family", false)
        );
        BookingRequest req = new BookingRequest(1L, null, passengers);

        Booking booking = bookingService.create(testCustomer, req);

        assertNotNull(booking);
        assertEquals(3, booking.getSeats().size());
        assertEquals(1500.0, booking.getTotalFare());
    }

    @Test
    void test3_WomenPreferenceBooking_Success() {
        when(busRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(testBus));
        when(seatRepo.findConfirmedSeatsByBusId(1L)).thenReturn(new ArrayList<>());
        when(bookingRepo.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PassengerRequest> passengers = List.of(
            new PassengerRequest(1, "Alice", "Female", "Individual", true)
        );
        BookingRequest req = new BookingRequest(1L, null, passengers);

        Booking booking = bookingService.create(testCustomer, req);

        assertNotNull(booking);
        assertTrue(booking.getSeats().get(0).isWomenPreference());
    }

    @Test
    void test4_AdjacentSeatPreference_Calculation() {
        Booking existingBooking = new Booking();
        existingBooking.setStatus(BookingStatus.CONFIRMED);
        BookingSeat bs1 = new BookingSeat(existingBooking, testBus, 1, "Alice", "Female", "Individual", true);

        Integer adjacent = Mapper.getAdjacentSeatNumber(bs1.getSeatNumber(), testBus.getTotalSeats());
        assertEquals(2, adjacent);
    }

    @Test
    void test5_RejectionOfWomenPreferred_ForGeneralPassenger() {
        Booking existingBooking = new Booking();
        existingBooking.setStatus(BookingStatus.CONFIRMED);
        BookingSeat bs1 = new BookingSeat(existingBooking, testBus, 1, "Alice", "Female", "Individual", true);

        when(busRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(testBus));
        when(seatRepo.findConfirmedSeatsByBusId(1L)).thenReturn(List.of(bs1));

        // General passenger trying to book Seat 2 (adjacent to Seat 1 which has womenPreference=true)
        List<PassengerRequest> passengers = List.of(
            new PassengerRequest(2, "Bob", "Male", "Individual", false)
        );
        BookingRequest req = new BookingRequest(1L, null, passengers);

        ApiException ex = assertThrows(ApiException.class, () -> bookingService.create(testCustomer, req));
        assertTrue(ex.getMessage().contains("prefered for womens only"));
    }

    @Test
    void test6_NoAdjacentSeatCase_Success() {
        when(busRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(testBus));
        when(seatRepo.findConfirmedSeatsByBusId(1L)).thenReturn(new ArrayList<>());
        when(bookingRepo.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Seat 40 on a 40-seat bus (adjacent is 39)
        List<PassengerRequest> passengers = List.of(
            new PassengerRequest(40, "Alice", "Female", "Individual", true)
        );
        BookingRequest req = new BookingRequest(1L, null, passengers);

        Booking booking = bookingService.create(testCustomer, req);
        assertNotNull(booking);
        assertEquals(40, booking.getSeats().get(0).getSeatNumber());
    }

    @Test
    void test7_DuplicateBookingPrevention() {
        Booking existingBooking = new Booking();
        existingBooking.setStatus(BookingStatus.CONFIRMED);
        BookingSeat bs1 = new BookingSeat(existingBooking, testBus, 5, "Dave", "Male", "Individual", false);

        when(busRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(testBus));
        when(seatRepo.findConfirmedSeatsByBusId(1L)).thenReturn(List.of(bs1));

        // Attempting to book already booked seat 5
        List<PassengerRequest> passengers = List.of(
            new PassengerRequest(5, "Charlie", "Male", "Individual", false)
        );
        BookingRequest req = new BookingRequest(1L, null, passengers);

        ApiException ex = assertThrows(ApiException.class, () -> bookingService.create(testCustomer, req));
        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    void test8_PartialCancellation_Success() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomer(testCustomer);
        booking.setBus(testBus);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalFare(1000.0);

        BookingSeat s1 = new BookingSeat(booking, testBus, 1, "Alice", "Female", "Individual", false);
        BookingSeat s2 = new BookingSeat(booking, testBus, 2, "Bob", "Male", "Individual", false);
        booking.getSeats().add(s1);
        booking.getSeats().add(s2);

        when(bookingRepo.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepo.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Cancel only 1 ticket (Seat 1)
        CancelBookingRequest req = new CancelBookingRequest(List.of(1), 1);
        Booking result = bookingService.cancel(testCustomer, 100L, req);

        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(1, result.getSeats().size());
        assertEquals(2, result.getSeats().get(0).getSeatNumber());
        assertEquals(500.0, result.getTotalFare());
        verify(seatRepo).deleteAll(anyList());
    }

    @Test
    void test9_MedicalAssistanceBooking_Success() {
        when(busRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(testBus));
        when(seatRepo.findConfirmedSeatsByBusId(1L)).thenReturn(new ArrayList<>());
        when(bookingRepo.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PassengerRequest> passengers = List.of(
            new PassengerRequest(1, "Grandma Doe", "Female", "Individual", false)
        );
        BookingRequest req = new BookingRequest(1L, null, passengers, true, "Wheelchair required at boarding");

        Booking booking = bookingService.create(testCustomer, req);

        assertNotNull(booking);
        assertTrue(booking.isMedicalAssistance());
        assertEquals("Wheelchair required at boarding", booking.getMedicalIssueDetails());
    }
}

