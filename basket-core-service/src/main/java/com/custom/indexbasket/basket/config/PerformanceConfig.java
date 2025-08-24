package com.custom.indexbasket.basket.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableR2dbcRepositories
public class PerformanceConfig extends AbstractR2dbcConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceConfig.class);
    
    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;
    
    @Value("${spring.r2dbc.username}")
    private String username;
    
    @Value("${spring.r2dbc.password}")
    private String password;
    
    @Value("${spring.r2dbc.pool.initial-size:5}")
    private int initialSize;
    
    @Value("${spring.r2dbc.pool.max-size:20}")
    private int maxSize;
    
    @Value("${spring.r2dbc.pool.max-idle-time:30}")
    private int maxIdleTime;
    
    @Value("${spring.r2dbc.pool.max-acquire-time:30}")
    private int maxAcquireTime;
    
    @Value("${spring.r2dbc.pool.max-lifetime:3600}")
    private int maxLifetime;

    public PerformanceConfig() {
        log.info("🔧 PerformanceConfig constructor called - Performance optimizations being loaded!");
    }

    @Override
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        log.info("🔧 Creating optimized ConnectionFactory with connection pooling...");
        
        String url = r2dbcUrl.replace("r2dbc:postgresql://", "");
        String[] parts = url.split("/");
        String[] hostPort = parts[0].split(":");
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);
        String database = parts[1];
        
        log.info("🔧 Database connection details - Host: {}, Port: {}, Database: {}", host, port, database);
        log.info("🔧 Connection pool settings - Initial: {}, Max: {}, MaxIdle: {}s, MaxAcquire: {}s, MaxLifetime: {}s", 
                initialSize, maxSize, maxIdleTime, maxAcquireTime, maxLifetime);
        
        PostgresqlConnectionFactory postgresqlConnectionFactory = new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .codecRegistrar(EnumCodec.builder().withEnum("basket_status", com.custom.indexbasket.common.model.BasketStatus.class).build())
                .build()
        );
        
        // Configure connection pool for performance
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(postgresqlConnectionFactory)
            .initialSize(initialSize)
            .maxSize(maxSize)
            .maxIdleTime(Duration.ofSeconds(maxIdleTime))
            .maxAcquireTime(Duration.ofSeconds(maxAcquireTime))
            .build();
        
        return new ConnectionPool(poolConfig);
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        log.info("🔧 Creating optimized R2dbcEntityTemplate bean...");
        return new R2dbcEntityTemplate(connectionFactory);
    }

    @Override
    protected List<Object> getCustomConverters() {
        log.info("🔧 getCustomConverters() called - Registering custom converters!");
        List<Object> converters = new ArrayList<>();
        converters.add(new com.custom.indexbasket.basket.config.BasketStatusReadingConverter());
        converters.add(new com.custom.indexbasket.basket.config.BasketStatusWritingConverter());
        log.info("🔧 Registered {} custom converters", converters.size());
        return converters;
    }
    
    /**
     * Configure optimized schedulers for different operation types
     */
    @Bean
    public SchedulersConfig schedulersConfig() {
        log.info("🔧 Configuring optimized schedulers for different operation types...");
        return new SchedulersConfig();
    }
    
    public static class SchedulersConfig {
        public static final String DATABASE_SCHEDULER = "database-scheduler";
        public static final String COMPUTATION_SCHEDULER = "computation-scheduler";
        public static final String IO_SCHEDULER = "io-scheduler";
        
        // Database operations scheduler (bounded elastic for database connections)
        @Bean(DATABASE_SCHEDULER)
        public reactor.core.scheduler.Scheduler databaseScheduler() {
            return Schedulers.boundedElastic();
        }
        
        // Computation scheduler (parallel for CPU-intensive tasks)
        @Bean(COMPUTATION_SCHEDULER)
        public reactor.core.scheduler.Scheduler computationScheduler() {
            return Schedulers.parallel();
        }
        
        // IO scheduler (bounded elastic for external API calls)
        @Bean(IO_SCHEDULER)
        public reactor.core.scheduler.Scheduler ioScheduler() {
            return Schedulers.boundedElastic();
        }
    }
}
