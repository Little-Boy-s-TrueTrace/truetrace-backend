package com.example.bank.controller;

import com.example.bank.config.JwtTokenUtil;
import com.example.bank.model.Account;
import com.example.bank.model.User;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // In-memory rate limiting map: IP -> List of failure timestamps
    private static final Map<String, List<Long>> loginFailures = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long BLOCK_WINDOW_MS = 60000; // 1 minute

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String fullName = request.get("fullName");
        String email = request.get("email");

        if (username == null || password == null || fullName == null || email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }

        username = username.trim();
        password = password.trim();
        fullName = fullName.trim();
        email = email.trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fields cannot be blank"));
        }

        if (!username.matches("^[a-zA-Z0-9_.-]{3,30}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be 3-30 characters and contain only letters, numbers, underscores, hyphens, or periods"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters long"));
        }

        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
        }

        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .email(email)
                .role("USER")
                .build();
        userRepository.save(user);

        Random rand = new Random();
        String accNum = "ACC-" + (100000 + rand.nextInt(900000));
        while (accountRepository.findByAccountNumber(accNum).isPresent()) {
            accNum = "ACC-" + (100000 + rand.nextInt(900000));
        }

        Account account = Account.builder()
                .accountNumber(accNum)
                .balance(20000000.0)
                .currency("VND")
                .user(user)
                .build();
        accountRepository.save(account);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully",
                "accountNumber", accNum
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        String username = request.get("username");
        String password = request.get("password");
        String clientIp = getClientIp(servletRequest);

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing username or password"));
        }

        // Rate Limiting Check
        long now = System.currentTimeMillis();
        List<Long> failures = loginFailures.computeIfAbsent(clientIp, k -> new ArrayList<>());
        failures.removeIf(timestamp -> now - timestamp > BLOCK_WINDOW_MS);

        if (failures.size() >= MAX_FAILED_ATTEMPTS) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many failed attempts. Please try again after 1 minute."
            ));
        }

        // Authenticate using prepared statement & BCrypt
        Optional<User> userOpt = userRepository.findByUsername(username);
        User authenticatedUser = null;
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            authenticatedUser = userOpt.get();
        }

        if (authenticatedUser == null) {
            failures.add(System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        // Login Success: Reset rate limits
        loginFailures.remove(clientIp);

        // Generate Token
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                authenticatedUser.getUsername(),
                authenticatedUser.getPassword(),
                new ArrayList<>()
        );
        String token = jwtTokenUtil.generateToken(userDetails);

        // Fetch Account Number
        Optional<Account> accountOpt = accountRepository.findByUser(authenticatedUser);
        String accountNumber = accountOpt.map(Account::getAccountNumber).orElse("NONE");

        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "id", authenticatedUser.getId(),
                        "username", authenticatedUser.getUsername(),
                        "fullName", authenticatedUser.getFullName(),
                        "email", authenticatedUser.getEmail(),
                        "role", authenticatedUser.getRole(),
                        "accountNumber", accountNumber
                )
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtTokenUtil.blacklistToken(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }
}
