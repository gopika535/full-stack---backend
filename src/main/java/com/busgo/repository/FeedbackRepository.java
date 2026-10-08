package com.busgo.repository;

import com.busgo.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    Optional<Feedback> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
}
