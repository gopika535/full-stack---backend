package com.busgo.repository;

import com.busgo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByBookedAtDesc();
    List<Booking> findByCustomerIdOrderByBookedAtDesc(Long customerId);
    List<Booking> findByBusIdOrderByBookedAtDesc(Long busId);
    List<Booking> findByBusOperatorIdOrderByBookedAtDesc(Long operatorId);
    long countByBusId(Long busId);
    long countByCustomerId(Long customerId);

    @Query("select coalesce(sum(b.totalFare), 0) from Booking b where b.status = com.busgo.entity.BookingStatus.CONFIRMED")
    double totalRevenue();

    @Query("select count(b) from Booking b where b.status = com.busgo.entity.BookingStatus.CONFIRMED")
    long countConfirmed();

    @Query("select b from Booking b join fetch b.bus bus join fetch b.customer c " +
           "where b.status = com.busgo.entity.BookingStatus.CONFIRMED " +
           "and b.journeyReminderSent = false " +
           "and bus.departureTime <= :threshold " +
           "and bus.departureTime >= :minDeparture")
    List<Booking> findPendingReminders(@org.springframework.data.repository.query.Param("threshold") java.time.LocalDateTime threshold,
                                       @org.springframework.data.repository.query.Param("minDeparture") java.time.LocalDateTime minDeparture);
}
