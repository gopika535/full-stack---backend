package com.busgo.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/** All request/response payloads of the REST API. */
public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record RegisterRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Mobile is required") @Pattern(regexp = "^[0-9+\\- ]{10,15}$", message = "Enter a valid mobile number") String mobile,
            @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record AuthResponse(String token, Long id, String name, String email, String mobile, String role) {}

    // ---------- users / operators ----------
    public record OperatorRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Mobile is required") String mobile,
            String password) {}

    /** count = number of buses (operators) or number of bookings (customers). */
    public record UserResponse(Long id, String name, String email, String mobile, String role, long count) {}

    // ---------- buses ----------
    public record BusRequest(
            @NotBlank(message = "Bus name is required") String name,
            @NotBlank(message = "Source is required") String source,
            @NotBlank(message = "Destination is required") String destination,
            @NotNull(message = "Departure time is required") LocalDateTime departureTime,
            @Min(value = 30, message = "Duration must be at least 30 minutes") int durationMinutes,
            @Min(value = 1, message = "Total seats must be at least 1") @Max(value = 80, message = "Total seats cannot exceed 80") int totalSeats,
            @Positive(message = "Fare must be greater than 0") double fare,
            String busType,
            String amenities,
            @NotNull(message = "Operator is required") Long operatorId) {}

    public record BusResponse(
            Long id, String name, String source, String destination,
            LocalDateTime departureTime, LocalDateTime arrivalTime, int durationMinutes,
            int totalSeats, int availableSeats, int bookedCount, double fare,
            String busType, String amenities, String status,
            Long operatorId, String operatorName, long bookingCount, List<Integer> bookedSeats,
            List<Integer> womenPreferredSeats,
            List<Integer> womenBookedSeats) {}

    public record OperatorBusDetail(BusResponse bus, List<BookingResponse> bookings) {}

    // ---------- bookings ----------
    public record PassengerRequest(
            @NotNull(message = "Seat number is required") Integer seatNumber,
            String name,
            String gender,
            String passengerType,
            boolean womenPreference) {}

    public record PassengerResponse(
            int seatNumber,
            String name,
            String gender,
            String passengerType,
            boolean womenPreference) {}

    public record BookingRequest(
            @NotNull(message = "Bus is required") Long busId,
            List<Integer> seatNumbers,
            List<PassengerRequest> passengers,
            boolean medicalAssistance,
            String medicalIssueDetails) {
        public BookingRequest(Long busId, List<Integer> seatNumbers, List<PassengerRequest> passengers) {
            this(busId, seatNumbers, passengers, false, null);
        }
    }

    public record CancelBookingRequest(List<Integer> seatNumbers, Integer ticketCount) {}

    public record BookingResponse(
            Long id, String bookingCode,
            Long busId, String busName, String source, String destination,
            LocalDateTime departureTime, LocalDateTime arrivalTime, String operatorName,
            Long customerId, String customerName, String customerEmail, String customerMobile,
            List<Integer> seats, List<PassengerResponse> passengers, double totalFare, String status, LocalDateTime bookedAt,
            boolean journeyCompleted, boolean feedbackGiven, Integer feedbackRating, String feedbackComment,
            boolean medicalAssistance, String medicalIssueDetails, boolean journeyReminderSent) {
        public BookingResponse(Long id, String bookingCode,
                Long busId, String busName, String source, String destination,
                LocalDateTime departureTime, LocalDateTime arrivalTime, String operatorName,
                Long customerId, String customerName, String customerEmail, String customerMobile,
                List<Integer> seats, List<PassengerResponse> passengers, double totalFare, String status, LocalDateTime bookedAt,
                boolean journeyCompleted, boolean feedbackGiven, Integer feedbackRating, String feedbackComment,
                boolean medicalAssistance, String medicalIssueDetails) {
            this(id, bookingCode, busId, busName, source, destination, departureTime, arrivalTime, operatorName,
                 customerId, customerName, customerEmail, customerMobile, seats, passengers, totalFare, status, bookedAt,
                 journeyCompleted, feedbackGiven, feedbackRating, feedbackComment, medicalAssistance, medicalIssueDetails, false);
        }
    }

    // ---------- notifications / feedback ----------
    public record NotificationResponse(Long id, String message, String type, String details, boolean read, LocalDateTime createdAt) {}

    public record FeedbackRequest(
            @NotNull(message = "Booking is required") Long bookingId,
            @Min(value = 1, message = "Rating must be between 1 and 5") @Max(value = 5, message = "Rating must be between 1 and 5") int rating,
            @Size(max = 1000, message = "Feedback is too long") String comment) {}

    public record FeedbackResponse(Long id, Long bookingId, String bookingCode, String customerName, String customerEmail,
                                   String busName, String route, int rating, String comment, LocalDateTime createdAt) {}

    // ---------- admin ----------
    public record StatsResponse(long totalCustomers, long totalOperators, long totalBuses, long activeBuses,
                                long cancelledBuses, long totalBookings, long confirmedBookings,
                                double revenue, long totalFeedback) {}
}
