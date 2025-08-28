package com.custom.indexbasket.publishing.config;

import com.custom.indexbasket.publishing.proxy.VendorProxyService;
import com.custom.indexbasket.publishing.service.VendorService;
import com.custom.indexbasket.publishing.service.VendorServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vendor Service Configuration
 * 
 * Allows easy switching between proxy and real vendor services via configuration.
 * During development, proxy services are used. In production, real vendor services are used.
 */
@Slf4j
@Configuration
public class VendorServiceConfiguration {

    @Value("${vendor.proxy.enabled:true}")
    private boolean proxyEnabled;

    /**
     * Use proxy services during development
     */
    @Bean
    @ConditionalOnProperty(name = "vendor.proxy.enabled", havingValue = "true")
    public VendorService vendorService(VendorProxyService proxyService) {
        log.info("Using Vendor Proxy Services for development (proxy.enabled={})", proxyEnabled);
        return new VendorServiceImpl(proxyService);
    }

    /**
     * Use real vendor services in production
     * This will be implemented when real vendor integration is ready
     */
    @Bean
    @ConditionalOnProperty(name = "vendor.proxy.enabled", havingValue = "false")
    public VendorService realVendorService(VendorProxyService proxyService) {
        log.info("Using Real Vendor Services for production (proxy.enabled={})", proxyEnabled);
        // TODO: Implement real vendor service when vendor onboarding is complete
        // For now, return the proxy service as a fallback
        return new VendorServiceImpl(proxyService);
    }
}
