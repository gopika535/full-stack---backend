package com.busgo.repository;

import com.busgo.entity.Bus;
import com.busgo.entity.BusStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findAllByOrderByDepartureTimeDesc();
    List<Bus> findByOperatorIdOrderByDepartureTimeAsc(Long operatorId);
    long countByOperatorId(Long operatorId);
    long countByStatus(BusStatus status);

    /** Row lock on the bus so concurrent bookings for the same bus are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bus b where b.id = :id")
    Optional<Bus> findByIdForUpdate(@Param("id") Long id);

    @Query("select b from Bus b where b.status = com.busgo.entity.BusStatus.ACTIVE " +
           "and b.departureTime > :now and b.departureTime >= :start and b.departureTime < :end " +
           "and lower(b.source) like lower(concat('%', :src, '%')) " +
           "and lower(b.destination) like lower(concat('%', :dst, '%')) " +
           "order by b.departureTime asc")
    List<Bus> search(@Param("src") String src, @Param("dst") String dst,
                     @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                     @Param("now") LocalDateTime now);
}
