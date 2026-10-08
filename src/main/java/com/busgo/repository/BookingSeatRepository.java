package com.busgo.repository;

import com.busgo.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    @Query("select bs.seatNumber from BookingSeat bs where bs.bus.id = :busId " +
           "and bs.booking.status = com.busgo.entity.BookingStatus.CONFIRMED order by bs.seatNumber")
    List<Integer> findBookedSeatNumbers(@Param("busId") Long busId);

    @Query("select bs from BookingSeat bs where bs.bus.id = :busId " +
           "and bs.booking.status = com.busgo.entity.BookingStatus.CONFIRMED")
    List<BookingSeat> findConfirmedSeatsByBusId(@Param("busId") Long busId);
}
