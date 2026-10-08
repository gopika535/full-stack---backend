package com.busgo.entity;

import jakarta.persistence.*;

/** One row per booked seat. The unique (bus_id, seat_number) constraint is the database-level guard against double booking. */
@Entity
@Table(name = "booking_seats",
       uniqueConstraints = @UniqueConstraint(name = "uk_bus_seat", columnNames = {"bus_id", "seat_number"}))
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "passenger_name")
    private String passengerName;

    @Column(name = "passenger_gender")
    private String passengerGender;

    @Column(name = "passenger_type")
    private String passengerType;

    @Column(name = "women_preference")
    private boolean womenPreference = false;

    public BookingSeat() {}
    public BookingSeat(Booking booking, Bus bus, int seatNumber) {
        this.booking = booking; this.bus = bus; this.seatNumber = seatNumber;
        if (booking != null && booking.getCustomer() != null) {
            this.passengerName = booking.getCustomer().getName();
        } else {
            this.passengerName = "Passenger";
        }
        this.passengerGender = "Prefer not to say";
        this.passengerType = "General Passenger";
        this.womenPreference = false;
    }

    public BookingSeat(Booking booking, Bus bus, int seatNumber, String passengerName, String passengerGender, String passengerType, boolean womenPreference) {
        this.booking = booking;
        this.bus = bus;
        this.seatNumber = seatNumber;
        this.passengerName = passengerName;
        this.passengerGender = passengerGender;
        this.passengerType = passengerType;
        this.womenPreference = womenPreference;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public String getPassengerGender() { return passengerGender; }
    public void setPassengerGender(String passengerGender) { this.passengerGender = passengerGender; }
    public String getPassengerType() { return passengerType; }
    public void setPassengerType(String passengerType) { this.passengerType = passengerType; }
    public boolean isWomenPreference() { return womenPreference; }
    public void setWomenPreference(boolean womenPreference) { this.womenPreference = womenPreference; }
}
