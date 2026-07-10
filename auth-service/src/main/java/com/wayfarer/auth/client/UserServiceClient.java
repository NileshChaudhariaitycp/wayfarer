package com.wayfarer.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative REST client (OpenFeign) — the interface below is all we write;
 * Spring generates the HTTP call at runtime. "user-service" is resolved via
 * Eureka + client-side load balancing, not a hardcoded host:port.
 *
 * Note the CreateProfileRequest here is auth-service's own copy of the
 * contract, not a shared DTO module. That's deliberate: a shared DTO jar
 * would let a change to user-service's internal model silently break
 * auth-service at compile time in a different repo/module — duplicating the
 * (small) contract keeps the services independently deployable, at the cost
 * of having to update both sides by hand if the contract changes.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @PostMapping("/internal/users")
    void createProfile(@RequestBody CreateProfileRequest request);
}
