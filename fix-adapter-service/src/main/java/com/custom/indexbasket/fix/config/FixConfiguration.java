package com.custom.indexbasket.fix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * FIX Configuration
 * 
 * Configuration for FIX Bloomberg integration.
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "fix.bloomberg")
public class FixConfiguration {
    
    private Connection connection = new Connection();
    private Session session = new Session();
    private Authentication authentication = new Authentication();
    private Message message = new Message();
    private MarketData marketData = new MarketData();
    
    @Data
    public static class Connection {
        private String host;
        private int port;
        private String senderCompId;
        private String targetCompId;
        private boolean useSsl = true;
        private String sslCertPath;
    }
    
    @Data
    public static class Session {
        private String beginString = "FIX.4.4";
        private int heartbeatInterval = 30;
        private int connectionTimeout = 10;
        private int reconnectInterval = 30;
        private boolean useDataDictionary = true;
        private String dataDictionary = "/config/FIX44.xml";
        private String startTime = "00:00:00";
        private String endTime = "23:59:59";
        private String timeZone = "UTC";
    }
    
    @Data
    public static class Authentication {
        private String username;
        private String password;
        private String certPath;
    }
    
    @Data
    public static class Message {
        private int defaultAppVersion = 9;
        private String defaultEncoding = "UTF-8";
        private boolean validateMessages = true;
        private boolean logMessages = true;
        private boolean logHeartbeats = false;
    }
    
    @Data
    public static class MarketData {
        private boolean enabled = true;
        private String symbols;
        private String fields;
        private int updateFrequency = 1000;
    }
    
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        
        RedisSerializationContext<String, Object> serializationContext = 
            RedisSerializationContext.<String, Object>newSerializationContext()
                .key(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();
        
        return new ReactiveRedisTemplate<String, Object>(factory, serializationContext);
    }
}
