package com.busgo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "buses")
public class Bus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String source;
    @Column(nullable = false)
    private String destination;
    @Column(nullable = false)
    private LocalDateTime departureTime;
    @Column(nullable = false)
    private int durationMinutes;
    @Column(nullable = false)
    private int totalSeats;
    @Column(nullable = false)
    private double fare;
    private String busType;
    @Column(length = 500)
    private String amenities;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusStatus status = BusStatus.ACTIVE;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }
    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public BusStatus getStatus() { return status; }
    public void setStatus(BusStatus status) { this.status = status; }
    public User getOperator() { return operator; }
    public void setOperator(User operator) { this.operator = operator; }
}
