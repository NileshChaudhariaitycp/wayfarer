package com.wayfarer.gateway.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wayfarer.security")
public record GatewaySecurityProperties(List<String> publicPaths) {
}
