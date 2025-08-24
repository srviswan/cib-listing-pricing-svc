package com.custom.indexbasket.marketdata.proxy.quality.impl;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.quality.DataValidator;
import com.custom.indexbasket.marketdata.proxy.quality.model.ValidationError;
import com.custom.indexbasket.marketdata.proxy.quality.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates market data prices for quality and consistency.
 */
@Component
@Slf4j
public class PriceValidator implements DataValidator {
    
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000.00");
    private static final BigDecimal MAX_SPREAD_PERCENTAGE = new BigDecimal("50.0");
    
    @Override
    public ValidationResult validate(MarketDataResponse data) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Price validation rules
        validatePrice(data.getLastPrice(), "lastPrice", errors);
        validatePrice(data.getBidPrice(), "bidPrice", errors);
        validatePrice(data.getAskPrice(), "askPrice", errors);
        validatePrice(data.getOpenPrice(), "openPrice", errors);
        validatePrice(data.getHighPrice(), "highPrice", errors);
        validatePrice(data.getLowPrice(), "lowPrice", errors);
        
        // Bid-ask spread validation
        validateBidAskSpread(data.getBidPrice(), data.getAskPrice(), errors);
        
        // Price consistency validation
        validatePriceConsistency(data, errors);
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private void validatePrice(BigDecimal price, String fieldName, List<ValidationError> errors) {
        if (price != null) {
            if (price.compareTo(MIN_PRICE) < 0) {
                errors.add(new ValidationError(
                    "PRICE_TOO_LOW", 
                    String.format("%s price %s is below minimum threshold %s", fieldName, price, MIN_PRICE),
                    fieldName,
                    price,
                    "HIGH"
                ));
            }
            
            if (price.compareTo(MAX_PRICE) > 0) {
                errors.add(new ValidationError(
                    "PRICE_TOO_HIGH", 
                    String.format("%s price %s is above maximum threshold %s", fieldName, price, MAX_PRICE),
                    fieldName,
                    price,
                    "HIGH"
                ));
            }
        }
    }
    
    private void validateBidAskSpread(BigDecimal bidPrice, BigDecimal askPrice, List<ValidationError> errors) {
        if (bidPrice != null && askPrice != null) {
            if (askPrice.compareTo(bidPrice) <= 0) {
                errors.add(new ValidationError(
                    "INVALID_BID_ASK_SPREAD", 
                    "Ask price must be greater than bid price",
                    "bidAskSpread",
                    String.format("bid: %s, ask: %s", bidPrice, askPrice),
                    "CRITICAL"
                ));
            }
            
            // Calculate spread percentage
            BigDecimal spread = askPrice.subtract(bidPrice);
            BigDecimal midPrice = bidPrice.add(askPrice).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            BigDecimal spreadPercentage = spread.divide(midPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            if (spreadPercentage.compareTo(MAX_SPREAD_PERCENTAGE) > 0) {
                errors.add(new ValidationError(
                    "SPREAD_TOO_WIDE", 
                    String.format("Bid-ask spread %.2f%% exceeds maximum threshold %.2f%%", 
                        spreadPercentage, MAX_SPREAD_PERCENTAGE),
                    "bidAskSpread",
                    spreadPercentage,
                    "MEDIUM"
                ));
            }
        }
    }
    
    private void validatePriceConsistency(MarketDataResponse data, List<ValidationError> errors) {
        // High price should be >= Low price
        if (data.getHighPrice() != null && data.getLowPrice() != null) {
            if (data.getHighPrice().compareTo(data.getLowPrice()) < 0) {
                errors.add(new ValidationError(
                    "INVALID_HIGH_LOW_PRICES", 
                    "High price must be greater than or equal to low price",
                    "highLowPrices",
                    String.format("high: %s, low: %s", data.getHighPrice(), data.getLowPrice()),
                    "HIGH"
                ));
            }
        }
        
        // Last price should be between high and low
        if (data.getLastPrice() != null && data.getHighPrice() != null && data.getLowPrice() != null) {
            if (data.getLastPrice().compareTo(data.getHighPrice()) > 0 || 
                data.getLastPrice().compareTo(data.getLowPrice()) < 0) {
                errors.add(new ValidationError(
                    "LAST_PRICE_OUT_OF_RANGE", 
                    "Last price must be between high and low prices",
                    "lastPrice",
                    data.getLastPrice(),
                    "MEDIUM"
                ));
            }
        }
    }
    
    @Override
    public String getValidatorName() {
        return "PriceValidator";
    }
    
    @Override
    public String getValidationRules() {
        return "Validates price ranges, bid-ask spreads, and price consistency";
    }
}
