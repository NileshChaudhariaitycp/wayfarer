package com.wayfarer.flight.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.flight.dto.FlightResponse;
import java.util.List;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Only takes effect when spring.cache.type=redis (the "docker" environment
 * — see application.yml).
 *
 * We bind a type-specific Jackson serializer to the "flightSearch" cache
 * name instead of using GenericJackson2JsonRedisSerializer with Jackson's
 * polymorphic default typing. Two earlier attempts at the generic approach
 * both failed:
 *   1. Passing Spring's autoconfigured ObjectMapper as-is (needed for
 *      LocalDateTime/JSR-310 support) with no default typing meant cache
 *      reads had no type hint and deserialized elements as LinkedHashMap
 *      instead of FlightResponse, which then blew up when the controller
 *      tried to re-serialize them as FlightResponse.
 *   2. Calling activateDefaultTyping(..., As.PROPERTY) fixed that but
 *      produced inconsistent output — Jackson wraps bean-shaped values with
 *      an embedded "@class" property but falls back to array-wrapper
 *      notation (["java.math.BigDecimal", 289.99]) for scalar values whose
 *      declared type isn't in its short list of "natural" types (String,
 *      Boolean, Integer, Long, Double — notably NOT BigDecimal). That mixed
 *      format round-tripped inconsistently.
 *
 * Since this cache only ever holds List<FlightResponse>, there's no need
 * for embedded type metadata at all: we can tell Jackson the exact type up
 * front. This sidesteps default typing altogether, which is also generally
 * the safer choice — Jackson's polymorphic default typing is a well-known
 * deserialization-gadget (RCE) vector when the source of the JSON isn't
 * fully trusted, and is worth avoiding on principle even here where Redis
 * is a first-party cache we control.
 */
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer flightSearchCacheCustomizer(ObjectMapper objectMapper) {
        JavaType flightListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, FlightResponse.class);
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, flightListType);

        RedisCacheConfiguration flightSearchConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return builder -> builder.withCacheConfiguration("flightSearch", flightSearchConfig);
    }
}
