# TimescaleDB Implementation Guide

## Overview

This guide provides practical implementation details for deploying and using TimescaleDB in the Custom Index Basket Management Platform.

## Database Setup and Configuration

### Docker Compose Configuration

```yaml
version: '3.8'
services:
  postgres-timescale:
    image: timescale/timescaledb:latest-pg15
    environment:
      POSTGRES_DB: basket_management
      POSTGRES_USER: basket_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_INITDB_ARGS: "--auth-host=scram-sha-256"
    ports:
      - "5432:5432"
    volumes:
      - timescale_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    command: >
      postgres
      -c shared_buffers=8GB
      -c effective_cache_size=24GB
      -c work_mem=256MB
      -c maintenance_work_mem=2GB
      -c checkpoint_completion_target=0.9
      -c max_wal_size=4GB
      -c min_wal_size=1GB
      -c timescaledb.max_background_workers=8

volumes:
  timescale_data:
```

### Database Initialization Script

```sql
-- init-scripts/01-init-timescaledb.sql

-- Create TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create database schema
CREATE SCHEMA IF NOT EXISTS basket_data;
CREATE SCHEMA IF NOT EXISTS market_data;
CREATE SCHEMA IF NOT EXISTS analytics;

-- Create hypertables for time-series data
\i /docker-entrypoint-initdb.d/02-create-hypertables.sql

-- Create regular tables for application data
\i /docker-entrypoint-initdb.d/03-create-app-tables.sql

-- Create indexes
\i /docker-entrypoint-initdb.d/04-create-indexes.sql

-- Create continuous aggregates
\i /docker-entrypoint-initdb.d/05-create-continuous-aggregates.sql

-- Set up retention and compression policies
\i /docker-entrypoint-initdb.d/06-setup-policies.sql
```

### Spring Boot Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/basket_management
    username: basket_user
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver
    
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/basket_management
    username: basket_user
    password: ${POSTGRES_PASSWORD}
    
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 100
          order_inserts: true
          order_updates: true

# TimescaleDB specific configuration
timescaledb:
  chunk-time-interval:
    daily-data: 1 month
    intraday-data: 1 day
    basket-valuations: 1 day
  compression:
    enabled: true
    compress-after: 7 days
  retention:
    intraday-data: 2 years
    basket-valuations: 5 years
```

## Data Access Layer Implementation

### Repository Pattern with TimescaleDB

```java
package com.custom.indexbasket.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MarketDataRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Insert daily market data using efficient batch insert
     */
    public Mono<Integer> insertDailyPrices(List<DailyPrice> prices) {
        String sql = """
            INSERT INTO market_data.stock_prices_daily 
            (time, symbol, exchange, currency, sector, open, high, low, close, volume, 
             adjusted_close, dividend, split_ratio, data_source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (time, symbol) DO UPDATE SET
                open = EXCLUDED.open,
                high = EXCLUDED.high,
                low = EXCLUDED.low,
                close = EXCLUDED.close,
                volume = EXCLUDED.volume,
                adjusted_close = EXCLUDED.adjusted_close,
                dividend = EXCLUDED.dividend,
                split_ratio = EXCLUDED.split_ratio
            """;
        
        return Mono.fromCallable(() -> {
            List<Object[]> batchArgs = prices.stream()
                .map(this::mapToObjectArray)
                .collect(Collectors.toList());
                
            int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
            return Arrays.stream(results).sum();
        });
    }
    
    /**
     * Get historical prices with optimized query
     */
    public Flux<DailyPrice> getHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT time, symbol, exchange, currency, sector, 
                   open, high, low, close, volume, adjusted_close
            FROM market_data.stock_prices_daily 
            WHERE symbol = ? 
              AND time >= ? 
              AND time <= ?
            ORDER BY time
            """;
        
        return Flux.fromStream(
            jdbcTemplate.queryForList(sql, symbol, startDate, endDate).stream()
        ).map(this::mapToDailyPrice);
    }
    
    /**
     * Get real-time intraday data
     */
    public Flux<IntradayPrice> getIntradayPrices(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT time, symbol, exchange, currency, price, volume, bid, ask
            FROM market_data.stock_prices_intraday 
            WHERE symbol = ? 
              AND time >= ? 
              AND time <= ?
            ORDER BY time DESC
            LIMIT 1000
            """;
        
        return Flux.fromStream(
            jdbcTemplate.queryForList(sql, symbol, startTime, endTime).stream()
        ).map(this::mapToIntradayPrice);
    }
    
    /**
     * Get aggregated data using continuous aggregates
     */
    public Flux<DailyAggregate> getDailyAggregates(String symbol, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT day, symbol, open, high, low, close, volume, 
                   avg_price, price_volatility, tick_count
            FROM market_data.stock_daily_agg 
            WHERE symbol = ? 
              AND day >= ? 
              AND day <= ?
            ORDER BY day
            """;
        
        return Flux.fromStream(
            jdbcTemplate.queryForList(sql, symbol, startDate, endDate).stream()
        ).map(this::mapToDailyAggregate);
    }
    
    /**
     * Calculate portfolio performance using SQL window functions
     */
    public Mono<PortfolioPerformance> calculatePortfolioPerformance(String basketCode, 
                                                                   LocalDate startDate, 
                                                                   LocalDate endDate) {
        String sql = """
            WITH portfolio_daily AS (
                SELECT 
                    spd.time,
                    SUM(spd.adjusted_close * bc.weight / 100.0) AS portfolio_value
                FROM market_data.stock_prices_daily spd
                JOIN basket_data.basket_constituents bc ON spd.symbol = bc.symbol
                WHERE bc.basket_code = ?
                  AND spd.time >= ? 
                  AND spd.time <= ?
                GROUP BY spd.time
                ORDER BY spd.time
            ),
            returns_calc AS (
                SELECT 
                    time,
                    portfolio_value,
                    LAG(portfolio_value) OVER (ORDER BY time) AS prev_value,
                    (portfolio_value - LAG(portfolio_value) OVER (ORDER BY time)) / 
                    LAG(portfolio_value) OVER (ORDER BY time) AS daily_return
                FROM portfolio_daily
            )
            SELECT 
                COUNT(*) AS trading_days,
                MIN(time) AS start_date,
                MAX(time) AS end_date,
                FIRST_VALUE(portfolio_value) OVER (ORDER BY time) AS initial_value,
                LAST_VALUE(portfolio_value) OVER (ORDER BY time RANGE UNBOUNDED FOLLOWING) AS final_value,
                AVG(daily_return) AS avg_daily_return,
                STDDEV(daily_return) AS daily_volatility,
                (LAST_VALUE(portfolio_value) OVER (ORDER BY time RANGE UNBOUNDED FOLLOWING) - 
                 FIRST_VALUE(portfolio_value) OVER (ORDER BY time)) / 
                FIRST_VALUE(portfolio_value) OVER (ORDER BY time) * 100 AS total_return
            FROM returns_calc
            WHERE daily_return IS NOT NULL
            """;
        
        return Mono.fromCallable(() -> 
            jdbcTemplate.queryForObject(sql, this::mapToPortfolioPerformance, 
                basketCode, startDate, endDate)
        );
    }
    
    private Object[] mapToObjectArray(DailyPrice price) {
        return new Object[]{
            price.getTime(),
            price.getSymbol(),
            price.getExchange(),
            price.getCurrency(),
            price.getSector(),
            price.getOpen(),
            price.getHigh(),
            price.getLow(),
            price.getClose(),
            price.getVolume(),
            price.getAdjustedClose(),
            price.getDividend(),
            price.getSplitRatio(),
            price.getDataSource()
        };
    }
    
    private DailyPrice mapToDailyPrice(Map<String, Object> row) {
        return DailyPrice.builder()
            .time(((Timestamp) row.get("time")).toLocalDateTime())
            .symbol((String) row.get("symbol"))
            .exchange((String) row.get("exchange"))
            .currency((String) row.get("currency"))
            .sector((String) row.get("sector"))
            .open((BigDecimal) row.get("open"))
            .high((BigDecimal) row.get("high"))
            .low((BigDecimal) row.get("low"))
            .close((BigDecimal) row.get("close"))
            .volume((Long) row.get("volume"))
            .adjustedClose((BigDecimal) row.get("adjusted_close"))
            .build();
    }
}
```

### Service Layer with Reactive Programming

```java
package com.custom.indexbasket.service;

import com.custom.indexbasket.repository.MarketDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimescaleMarketDataService {
    
    @Autowired
    private MarketDataRepository repository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * Get historical data with caching
     */
    @Cacheable(value = "historicalPrices", key = "#symbol + '_' + #startDate + '_' + #endDate")
    public Flux<DailyPrice> getHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        return repository.getHistoricalPrices(symbol, startDate, endDate)
            .publishOn(Schedulers.parallel())
            .doOnSubscribe(sub -> log.info("Fetching historical prices for {} from {} to {}", 
                symbol, startDate, endDate))
            .doOnComplete(() -> log.info("Completed fetching historical prices for {}", symbol))
            .onErrorResume(throwable -> {
                log.error("Error fetching historical prices for {}: {}", symbol, throwable.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * Calculate basket historical performance
     */
    public Mono<BasketPerformance> calculateBasketPerformance(String basketCode, 
                                                             LocalDate startDate, 
                                                             LocalDate endDate) {
        return repository.calculatePortfolioPerformance(basketCode, startDate, endDate)
            .map(performance -> {
                // Calculate additional metrics
                double annualizedReturn = Math.pow(1 + performance.getTotalReturn() / 100, 
                    365.0 / performance.getTradingDays()) - 1;
                double annualizedVolatility = performance.getDailyVolatility() * Math.sqrt(252);
                double sharpeRatio = (annualizedReturn - 0.02) / annualizedVolatility; // Assuming 2% risk-free rate
                
                return BasketPerformance.builder()
                    .basketCode(basketCode)
                    .startDate(performance.getStartDate())
                    .endDate(performance.getEndDate())
                    .totalReturn(performance.getTotalReturn())
                    .annualizedReturn(annualizedReturn * 100)
                    .volatility(annualizedVolatility * 100)
                    .sharpeRatio(sharpeRatio)
                    .tradingDays(performance.getTradingDays())
                    .build();
            });
    }
    
    /**
     * Store real-time prices with batching
     */
    public Mono<Void> storeRealtimePrices(Flux<IntradayPrice> priceStream) {
        return priceStream
            .buffer(Duration.ofSeconds(5), 100) // Batch by time or count
            .flatMap(prices -> {
                return repository.insertIntradayPrices(prices)
                    .doOnSuccess(count -> log.debug("Stored {} intraday prices", count))
                    .then();
            })
            .doOnError(error -> log.error("Error storing real-time prices: {}", error.getMessage()))
            .onErrorResume(error -> Mono.empty())
            .then();
    }
    
    /**
     * Get current basket valuation
     */
    public Mono<BasketValuation> getCurrentBasketValuation(String basketCode) {
        String cacheKey = "basket:valuation:" + basketCode;
        
        // Try cache first
        return Mono.fromCallable(() -> redisTemplate.opsForValue().get(cacheKey))
            .flatMap(cached -> {
                if (cached != null) {
                    try {
                        return Mono.just(objectMapper.readValue(cached, BasketValuation.class));
                    } catch (Exception e) {
                        log.warn("Failed to deserialize cached valuation: {}", e.getMessage());
                        return Mono.empty();
                    }
                }
                return Mono.empty();
            })
            .switchIfEmpty(
                // Calculate from database if not in cache
                calculateCurrentBasketValuation(basketCode)
                    .doOnNext(valuation -> {
                        // Cache for 5 seconds
                        try {
                            String json = objectMapper.writeValueAsString(valuation);
                            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(5));
                        } catch (Exception e) {
                            log.warn("Failed to cache valuation: {}", e.getMessage());
                        }
                    })
            );
    }
    
    private Mono<BasketValuation> calculateCurrentBasketValuation(String basketCode) {
        String sql = """
            SELECT 
                bc.basket_code,
                SUM(spi.price * bc.weight / 100.0) AS total_value,
                COUNT(bc.symbol) AS constituent_count,
                MAX(spi.time) AS last_updated
            FROM basket_data.basket_constituents bc
            LEFT JOIN LATERAL (
                SELECT price, time
                FROM market_data.stock_prices_intraday spi_inner
                WHERE spi_inner.symbol = bc.symbol
                ORDER BY time DESC
                LIMIT 1
            ) spi ON true
            WHERE bc.basket_code = ?
            GROUP BY bc.basket_code
            """;
        
        return Mono.fromCallable(() -> 
            jdbcTemplate.queryForObject(sql, this::mapToBasketValuation, basketCode)
        ).onErrorReturn(BasketValuation.empty(basketCode));
    }
}
```

## Performance Monitoring and Optimization

### Performance Monitoring Queries

```sql
-- Monitor chunk statistics
SELECT 
    hypertable_name,
    chunk_name,
    table_size,
    index_size,
    total_size,
    compression_status
FROM timescaledb_information.chunks
WHERE hypertable_name = 'stock_prices_daily'
ORDER BY chunk_name DESC
LIMIT 10;

-- Monitor compression ratios
SELECT 
    hypertable_name,
    SUM(CASE WHEN compression_status = 'Compressed' THEN total_size ELSE 0 END) AS compressed_size,
    SUM(CASE WHEN compression_status = 'Uncompressed' THEN total_size ELSE 0 END) AS uncompressed_size,
    ROUND(
        SUM(CASE WHEN compression_status = 'Uncompressed' THEN total_size ELSE 0 END)::numeric / 
        NULLIF(SUM(CASE WHEN compression_status = 'Compressed' THEN total_size ELSE 0 END), 0),
        2
    ) AS compression_ratio
FROM timescaledb_information.chunks
WHERE hypertable_name IN ('stock_prices_daily', 'stock_prices_intraday')
GROUP BY hypertable_name;

-- Monitor query performance
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
WHERE query LIKE '%stock_prices%'
ORDER BY total_time DESC
LIMIT 10;

-- Monitor continuous aggregate refresh
SELECT 
    view_name,
    completed_threshold,
    invalidation_threshold,
    last_run_started_at,
    last_run_status,
    job_status,
    next_start
FROM timescaledb_information.continuous_aggregates ca
JOIN timescaledb_information.jobs j ON j.hypertable_name = ca.view_name;
```

### Optimization Strategies

```java
package com.custom.indexbasket.optimization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TimescaleOptimizationService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Automated maintenance tasks
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void performMaintenance() {
        log.info("Starting TimescaleDB maintenance tasks");
        
        // Update table statistics
        updateTableStatistics();
        
        // Refresh continuous aggregates
        refreshContinuousAggregates();
        
        // Analyze query performance
        analyzeQueryPerformance();
        
        log.info("Completed TimescaleDB maintenance tasks");
    }
    
    private void updateTableStatistics() {
        String[] tables = {
            "market_data.stock_prices_daily",
            "market_data.stock_prices_intraday", 
            "market_data.index_values",
            "market_data.fx_rates",
            "market_data.basket_valuations"
        };
        
        for (String table : tables) {
            try {
                jdbcTemplate.execute("ANALYZE " + table);
                log.debug("Updated statistics for table: {}", table);
            } catch (Exception e) {
                log.error("Failed to update statistics for table {}: {}", table, e.getMessage());
            }
        }
    }
    
    private void refreshContinuousAggregates() {
        String[] views = {
            "market_data.stock_daily_agg",
            "market_data.basket_weekly_performance",
            "market_data.index_monthly_performance",
            "market_data.fx_hourly_agg"
        };
        
        for (String view : views) {
            try {
                jdbcTemplate.execute("SELECT refresh_continuous_aggregate('" + view + "', NULL, NULL)");
                log.debug("Refreshed continuous aggregate: {}", view);
            } catch (Exception e) {
                log.error("Failed to refresh continuous aggregate {}: {}", view, e.getMessage());
            }
        }
    }
    
    private void analyzeQueryPerformance() {
        String sql = """
            SELECT 
                query,
                calls,
                total_time,
                mean_time,
                rows
            FROM pg_stat_statements 
            WHERE query LIKE '%stock_prices%' 
              AND mean_time > 1000  -- Queries taking more than 1 second
            ORDER BY total_time DESC
            LIMIT 5
            """;
        
        try {
            List<Map<String, Object>> slowQueries = jdbcTemplate.queryForList(sql);
            if (!slowQueries.isEmpty()) {
                log.warn("Found {} slow queries", slowQueries.size());
                slowQueries.forEach(query -> 
                    log.warn("Slow query: {} - Calls: {}, Mean time: {}ms", 
                        query.get("query"), query.get("calls"), query.get("mean_time"))
                );
            }
        } catch (Exception e) {
            log.error("Failed to analyze query performance: {}", e.getMessage());
        }
    }
    
    /**
     * Manual compression trigger for specific time ranges
     */
    public void compressTimeRange(String hypertable, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT compress_chunk(chunk) FROM show_chunks(?, ?, ?) AS chunk";
        
        try {
            List<String> chunks = jdbcTemplate.queryForList(sql, String.class, 
                hypertable, startTime, endTime);
            log.info("Compressed {} chunks for hypertable {}", chunks.size(), hypertable);
        } catch (Exception e) {
            log.error("Failed to compress chunks for hypertable {}: {}", hypertable, e.getMessage());
        }
    }
    
    /**
     * Reorder chunks by time for better query performance
     */
    public void reorderChunks(String hypertable) {
        String sql = """
            SELECT chunk_name 
            FROM timescaledb_information.chunks 
            WHERE hypertable_name = ? 
              AND compression_status = 'Uncompressed'
            ORDER BY range_start
            """;
        
        try {
            List<String> chunks = jdbcTemplate.queryForList(sql, String.class, hypertable);
            for (String chunk : chunks) {
                jdbcTemplate.execute("SELECT reorder_chunk('" + chunk + "', 'time')");
            }
            log.info("Reordered {} chunks for hypertable {}", chunks.size(), hypertable);
        } catch (Exception e) {
            log.error("Failed to reorder chunks for hypertable {}: {}", hypertable, e.getMessage());
        }
    }
}
```

## Deployment and Operations

### Kubernetes Deployment

```yaml
# k8s/timescaledb-deployment.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: timescaledb
spec:
  serviceName: timescaledb
  replicas: 1
  selector:
    matchLabels:
      app: timescaledb
  template:
    metadata:
      labels:
        app: timescaledb
    spec:
      containers:
      - name: timescaledb
        image: timescale/timescaledb:latest-pg15
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: basket_management
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: timescaledb-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: timescaledb-secret
              key: password
        args:
          - postgres
          - -c
          - shared_buffers=8GB
          - -c
          - effective_cache_size=24GB
          - -c
          - work_mem=256MB
          - -c
          - maintenance_work_mem=2GB
          - -c
          - timescaledb.max_background_workers=8
        volumeMounts:
        - name: timescaledb-storage
          mountPath: /var/lib/postgresql/data
        - name: init-scripts
          mountPath: /docker-entrypoint-initdb.d
        resources:
          requests:
            memory: "16Gi"
            cpu: "4"
          limits:
            memory: "32Gi"
            cpu: "8"
      volumes:
      - name: init-scripts
        configMap:
          name: timescaledb-init-scripts
  volumeClaimTemplates:
  - metadata:
      name: timescaledb-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: fast-ssd
      resources:
        requests:
          storage: 1Ti
```

### Backup and Recovery

```bash
#!/bin/bash
# backup-timescaledb.sh

# Configuration
DB_HOST="timescaledb-service"
DB_PORT="5432"
DB_NAME="basket_management"
DB_USER="basket_user"
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/timescaledb_backup_${DATE}.sql"

# Create backup directory
mkdir -p $BACKUP_DIR

# Full database backup
echo "Starting TimescaleDB backup at $(date)"
pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
    --verbose \
    --format=custom \
    --compress=9 \
    --file=$BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "Backup completed successfully: $BACKUP_FILE"
    
    # Compress backup
    gzip $BACKUP_FILE
    echo "Backup compressed: ${BACKUP_FILE}.gz"
    
    # Upload to cloud storage (optional)
    # aws s3 cp ${BACKUP_FILE}.gz s3://basket-backups/timescaledb/
    
    # Cleanup old backups (keep last 7 days)
    find $BACKUP_DIR -name "timescaledb_backup_*.sql.gz" -mtime +7 -delete
    
else
    echo "Backup failed!"
    exit 1
fi

echo "Backup process completed at $(date)"
```

This comprehensive TimescaleDB implementation guide provides all the necessary components for deploying and operating the unified database architecture in your basket management platform.
