package com.custom.indexbasket.basket.config;

import com.custom.indexbasket.common.model.BasketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Writing converter for BasketStatus enum
 * Converts BasketStatus enum to String values for database
 */
@WritingConverter
public class BasketStatusWritingConverter implements Converter<BasketStatus, String> {
    
    private static final Logger log = LoggerFactory.getLogger(BasketStatusWritingConverter.class);
    
    public BasketStatusWritingConverter() {
        log.info("🔧 BasketStatusWritingConverter constructor called!");
    }
    
    @Override
    public String convert(BasketStatus source) {
        log.info("🔧 Writing converter called with source: {}", source);
        
        if (source == null) {
            log.info("🔧 Source is null, returning null");
            return null;
        }
        String result = source.name();
        log.info("🔧 Successfully converted BasketStatus {} to String: '{}'", source, result);
        return result;
    }
}
