package ac.gachon.elasticacheapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {

    /**
     * Redis 서버 연결 설정
     * application.yml의 spring.redis.host/port를 자동 인식
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    /**
     * LocalDateTime 및 Hibernate Proxy 직렬화를 지원하는 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Hibernate6Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    /**
     * Spring Cache용 Redis Cache Manager 설정
     * - Key: String
     * - Value: JSON 직렬화 (LocalDateTime 지원)
     * - TTL(선택): 60분
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))  // 캐시 기본 TTL (핸즈온용)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                RedisSerializer.string()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                serializer
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}