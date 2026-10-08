package com.busgo.controller;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.Role;
import com.busgo.entity.User;
import com.busgo.exception.ApiException;
import com.busgo.repository.UserRepository;
import com.busgo.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    private AuthResponse auth(User u) {
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name()), u.getId(), u.getName(),
                u.getEmail(), u.getMobile(), u.getRole().name());
    }

    /** Public registration always creates a CUSTOMER. Admin/operators are created by the DataSeeder / Admin panel. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("This email is already registered");
        User u = users.save(new User(r.name().trim(), email, r.mobile().trim(), encoder.encode(r.password()), Role.CUSTOMER));
        return auth(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(r.password(), u.getPassword()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        return auth(u);
    }
}
