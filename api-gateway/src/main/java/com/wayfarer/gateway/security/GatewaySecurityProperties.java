package com.wayfarer.gateway.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * publicPaths: no token required, any HTTP method (login/register, health).
 * publicGetPaths: no token required for GET only — everything else on that
 * path (POST/PUT/DELETE) still requires a valid token. This is how flight/
 * hotel search stays public while their admin CRUD endpoints don't.
 */
@ConfigurationProperties(prefix = "wayfarer.security")
public record GatewaySecurityProperties(List<String> publicPaths, List<String> publicGetPaths) {
}
