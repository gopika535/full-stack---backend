package com.busgo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String bookingCode;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;
    @Column(nullable = false)
    private double totalFare;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;
    @Column(nullable = false)
    private LocalDateTime bookedAt = LocalDateTime.now();
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seatNumber ASC")
    private List<BookingSeat> seats = new ArrayList<>();

    @Column(name = "medical_assistance", nullable = false)
    private boolean medicalAssistance = false;

    @Column(name = "medical_issue_details", length = 1000)
    private String medicalIssueDetails;

    @Column(name = "journey_reminder_sent", nullable = false)
    private boolean journeyReminderSent = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    public double getTotalFare() { return totalFare; }
    public void setTotalFare(double totalFare) { this.totalFare = totalFare; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
    public List<BookingSeat> getSeats() { return seats; }
    public void setSeats(List<BookingSeat> seats) { this.seats = seats; }
    public boolean isMedicalAssistance() { return medicalAssistance; }
    public void setMedicalAssistance(boolean medicalAssistance) { this.medicalAssistance = medicalAssistance; }
    public String getMedicalIssueDetails() { return medicalIssueDetails; }
    public void setMedicalIssueDetails(String medicalIssueDetails) { this.medicalIssueDetails = medicalIssueDetails; }
    public boolean isJourneyReminderSent() { return journeyReminderSent; }
    public void setJourneyReminderSent(boolean journeyReminderSent) { this.journeyReminderSent = journeyReminderSent; }
}
