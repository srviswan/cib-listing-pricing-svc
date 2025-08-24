package com.custom.indexbasket.basket.config;

import com.custom.indexbasket.common.model.BasketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Reading converter for BasketStatus enum
 * Converts String values from database to BasketStatus enum
 */
@ReadingConverter
public class BasketStatusReadingConverter implements Converter<String, BasketStatus> {
    
    private static final Logger log = LoggerFactory.getLogger(BasketStatusReadingConverter.class);
    
    public BasketStatusReadingConverter() {
        log.info("🔧 BasketStatusReadingConverter constructor called!");
    }
    
    @Override
    public BasketStatus convert(String source) {
        log.info("🔧 Reading converter called with source: '{}'", source);
        
        if (source == null || source.trim().isEmpty()) {
            log.info("🔧 Source is null or empty, returning null");
            return null;
        }
        try {
            BasketStatus result = BasketStatus.valueOf(source.toUpperCase());
            log.info("🔧 Successfully converted '{}' to BasketStatus: {}", source, result);
            return result;
        } catch (IllegalArgumentException e) {
            log.error("🔧 Error converting '{}' to BasketStatus: {}", source, e.getMessage());
            throw new IllegalArgumentException("Invalid basket status: " + source, e);
        }
    }
}
