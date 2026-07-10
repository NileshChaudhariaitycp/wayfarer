package com.wayfarer.auth.service;

import com.wayfarer.auth.client.CreateProfileRequest;
import com.wayfarer.auth.client.UserServiceClient;
import com.wayfarer.auth.dto.AuthResponse;
import com.wayfarer.auth.dto.LoginRequest;
import com.wayfarer.auth.dto.RegisterRequest;
import com.wayfarer.auth.entity.Credential;
import com.wayfarer.auth.entity.Role;
import com.wayfarer.auth.exception.DuplicateUsernameException;
import com.wayfarer.auth.exception.InvalidCredentialsException;
import com.wayfarer.auth.repository.CredentialRepository;
import com.wayfarer.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (credentialRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }

        Credential credential = new Credential();
        credential.setUsername(request.username());
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credential.setRole(Role.CUSTOMER);
        credential.setEnabled(true);
        credential = credentialRepository.save(credential);
        log.info("Registered new credential for username={} role={}", credential.getUsername(), credential.getRole());

        // Known simplification: this is a synchronous cross-service call inside
        // the same transaction boundary as the credential save above. If
        // user-service is unreachable, the credential now exists with no
        // profile — a real inconsistent state. This is exactly the class of
        // problem booking-service's Saga pattern (Phase 4) exists to solve
        // properly, with compensating actions instead of hoping nothing fails.
        userServiceClient.createProfile(new CreateProfileRequest(
                credential.getId(),
                credential.getUsername(),
                request.email(),
                request.fullName(),
                credential.getRole().name()
        ));

        String token = jwtService.generateToken(credential);
        return new AuthResponse(token, credential.getUsername(), credential.getRole().name(),
                System.currentTimeMillis() + jwtService.expirationMs());
    }

    public AuthResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByUsername(request.username())
                .filter(Credential::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("Login succeeded for username={}", credential.getUsername());
        String token = jwtService.generateToken(credential);
        return new AuthResponse(token, credential.getUsername(), credential.getRole().name(),
                System.currentTimeMillis() + jwtService.expirationMs());
    }
}
