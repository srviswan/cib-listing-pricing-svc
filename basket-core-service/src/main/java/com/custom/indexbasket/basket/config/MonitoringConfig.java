package com.custom.indexbasket.basket.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableAspectJAutoProxy
public class MonitoringConfig {
    
    private static final Logger log = LoggerFactory.getLogger(MonitoringConfig.class);
    
    public MonitoringConfig() {
        log.info("🔧 MonitoringConfig constructor called - Monitoring and metrics being configured!");
    }

    /**
     * Enable timing aspects for method performance monitoring
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.info("🔧 Configuring TimedAspect for method performance monitoring...");
        return new TimedAspect(registry);
    }

    /**
     * Customize meter registry with application tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        log.info("🔧 Configuring common metrics tags...");
        return registry -> registry.config()
            .commonTags("application", "basket-core-service")
            .commonTags("version", "1.0.0")
            .commonTags("environment", "development");
    }

    /**
     * Custom performance metrics for basket operations
     */
    @Bean
    public BasketMetrics basketMetrics(MeterRegistry meterRegistry) {
        log.info("🔧 Configuring custom basket operation metrics...");
        return new BasketMetrics(meterRegistry);
    }
    
    public static class BasketMetrics {
        public final Timer createBasketTimer;
        public final Timer readBasketTimer;
        public final Timer updateBasketTimer;
        public final Timer deleteBasketTimer;
        public final Timer listBasketsTimer;
        
        public BasketMetrics(MeterRegistry meterRegistry) {
            this.createBasketTimer = Timer.builder("basket.operations.create")
                .description("Time taken to create a basket")
                .register(meterRegistry);
                
            this.readBasketTimer = Timer.builder("basket.operations.read")
                .description("Time taken to read a basket")
                .register(meterRegistry);
                
            this.updateBasketTimer = Timer.builder("basket.operations.update")
                .description("Time taken to update a basket")
                .register(meterRegistry);
                
            this.deleteBasketTimer = Timer.builder("basket.operations.delete")
                .description("Time taken to delete a basket")
                .register(meterRegistry);
                
            this.listBasketsTimer = Timer.builder("basket.operations.list")
                .description("Time taken to list baskets")
                .register(meterRegistry);
        }
        
        public Timer.Sample startCreateBasketTimer() {
            return Timer.start();
        }
        
        public void stopCreateBasketTimer(Timer.Sample sample) {
            sample.stop(createBasketTimer);
        }
        
        public Timer.Sample startReadBasketTimer() {
            return Timer.start();
        }
        
        public void stopReadBasketTimer(Timer.Sample sample) {
            sample.stop(readBasketTimer);
        }
        
        public Timer.Sample startUpdateBasketTimer() {
            return Timer.start();
        }
        
        public void stopUpdateBasketTimer(Timer.Sample sample) {
            sample.stop(updateBasketTimer);
        }
        
        public Timer.Sample startDeleteBasketTimer() {
            return Timer.start();
        }
        
        public void stopDeleteBasketTimer(Timer.Sample sample) {
            sample.stop(deleteBasketTimer);
        }
        
        public Timer.Sample startListBasketsTimer() {
            return Timer.start();
        }
        
        public void stopListBasketsTimer(Timer.Sample sample) {
            sample.stop(listBasketsTimer);
        }
    }
}
