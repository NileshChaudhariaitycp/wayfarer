package com.wayfarer.hotel.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.hotel.dto.HotelResponse;
import java.util.List;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/** See flight-service's CacheConfig for the full reasoning (type-specific serializer, why default typing was dropped) — same fix here. */
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer hotelSearchCacheCustomizer(ObjectMapper objectMapper) {
        JavaType hotelListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, HotelResponse.class);
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, hotelListType);

        RedisCacheConfiguration hotelSearchConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return builder -> builder.withCacheConfiguration("hotelSearch", hotelSearchConfig);
    }
}
