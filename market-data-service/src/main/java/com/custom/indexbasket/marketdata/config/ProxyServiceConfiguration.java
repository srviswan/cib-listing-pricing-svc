package com.custom.indexbasket.marketdata.config;

import com.custom.indexbasket.marketdata.proxy.DataSourceProxy;
import com.custom.indexbasket.marketdata.proxy.impl.BloombergProxy;
import com.custom.indexbasket.marketdata.proxy.config.DataSourceConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for proxy services.
 * Only loaded when not in minimal mode.
 */
@Configuration
@ConditionalOnProperty(name = "market.data.minimal", havingValue = "false", matchIfMissing = true)
public class ProxyServiceConfiguration {
    
    /**
     * Configure Bloomberg proxy service
     * Only loaded when not in minimal mode
     */
    @Bean
    public DataSourceProxy bloombergProxy(DataSourceConfig config, MeterRegistry meterRegistry) {
        return new BloombergProxy(config, meterRegistry);
    }
}
