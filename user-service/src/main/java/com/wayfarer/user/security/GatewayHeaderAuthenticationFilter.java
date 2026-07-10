package com.wayfarer.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * "Trust the gateway": api-gateway validates the JWT once and forwards
 * X-User-Id / X-User-Roles headers. This filter reads those headers and
 * populates Spring Security's context from them — user-service never parses
 * a JWT itself.
 *
 * Explicit trade-off (called out in ADR 0004): this only holds if user-service
 * is unreachable except through the gateway. Anyone who can reach this service
 * directly can forge these headers and impersonate any user/role. Phase 7's
 * docker-compose only publishes the gateway's port to the host for exactly
 * this reason; production-grade defense-in-depth would also re-validate the
 * JWT here rather than trusting headers alone.
 */
@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String rolesHeader = request.getHeader(USER_ROLES_HEADER);

        if (userId != null && rolesHeader != null) {
            List<GrantedAuthority> authorities = List.of(rolesHeader.split(",")).stream()
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
