package com.busgo.repository;

import com.busgo.entity.Role;
import com.busgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleOrderByIdDesc(Role role);
    long countByRole(Role role);
}
