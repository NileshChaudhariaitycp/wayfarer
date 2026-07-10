package com.wayfarer.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The @Id here is NOT auto-generated — it's the same id as auth-service's
 * Credential.id, passed in by auth-service when it creates the profile after
 * registration. This is a "shared primary key" association across a service
 * boundary: it's how the two services agree "this profile belongs to that
 * credential" without a foreign key (which is impossible across separate
 * databases) or a shared users table (which would defeat the whole point of
 * splitting the services).
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
