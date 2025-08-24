package com.custom.indexbasket.marketdata.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Market Data Calculation Service - Provides analytics and risk calculations
 * 
 * This service provides:
 * - Portfolio analytics (beta, volatility, diversification)
 * - Risk scoring and assessment
 * - Performance metrics calculation
 * - Statistical analysis
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Service
public class MarketDataCalculationService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCalculationService.class);

    public MarketDataCalculationService() {
        // Default constructor
    }

    /**
     * Calculate weighted average of a metric across constituents
     */
    public BigDecimal calculateWeightedAverage(List<BigDecimal> values, List<BigDecimal> weights) {
        if (values == null || weights == null || values.size() != weights.size()) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            BigDecimal weight = weights.get(i);
            
            if (value != null && weight != null) {
                weightedSum = weightedSum.add(value.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }

        return totalWeight.compareTo(BigDecimal.ZERO) > 0 
            ? weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    }

    /**
     * Calculate portfolio Beta (weighted average of individual betas)
     */
    public BigDecimal calculatePortfolioBeta(List<BigDecimal> betas, List<BigDecimal> weights) {
        return calculateWeightedAverage(betas, weights);
    }

    /**
     * Calculate portfolio Volatility
     */
    public BigDecimal calculatePortfolioVolatility(List<BigDecimal> volatilities, List<BigDecimal> weights) {
        // Simplified portfolio volatility calculation
        // In practice, this would include correlation matrix
        return calculateWeightedAverage(volatilities, weights);
    }

    /**
     * Calculate sector diversification score
     */
    public BigDecimal calculateSectorDiversificationScore(List<String> sectors, List<BigDecimal> weights) {
        if (sectors == null || weights == null || sectors.size() != weights.size()) {
            return BigDecimal.ZERO;
        }

        // Calculate Herfindahl-Hirschman Index (HHI) for concentration
        BigDecimal hhi = BigDecimal.ZERO;
        BigDecimal totalWeight = weights.stream()
            .filter(w -> w != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            for (BigDecimal weight : weights) {
                if (weight != null) {
                    BigDecimal normalizedWeight = weight.divide(totalWeight, 4, RoundingMode.HALF_UP);
                    hhi = hhi.add(normalizedWeight.pow(2));
                }
            }
        }

        // Convert HHI to diversification score (0 = concentrated, 1 = diversified)
        // HHI ranges from 1/n to 1, where n is number of sectors
        BigDecimal maxHHI = BigDecimal.ONE;
        BigDecimal minHHI = BigDecimal.ONE.divide(BigDecimal.valueOf(sectors.size()), 4, RoundingMode.HALF_UP);
        
        if (maxHHI.compareTo(minHHI) > 0) {
            return maxHHI.subtract(hhi).divide(maxHHI.subtract(minHHI), 4, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Calculate geographic diversification score
     */
    public BigDecimal calculateGeographicDiversificationScore(List<String> countries, List<BigDecimal> weights) {
        // Similar to sector diversification
        return calculateSectorDiversificationScore(countries, weights);
    }

    /**
     * Calculate risk score based on multiple factors
     */
    public BigDecimal calculateRiskScore(BigDecimal beta, BigDecimal volatility, BigDecimal concentration) {
        // Normalize factors to 0-1 scale and calculate weighted risk score
        BigDecimal normalizedBeta = normalizeBeta(beta);
        BigDecimal normalizedVolatility = normalizeVolatility(volatility);
        BigDecimal normalizedConcentration = concentration; // Already 0-1

        // Weight factors (can be configurable)
        BigDecimal betaWeight = new BigDecimal("0.3");
        BigDecimal volatilityWeight = new BigDecimal("0.4");
        BigDecimal concentrationWeight = new BigDecimal("0.3");

        return normalizedBeta.multiply(betaWeight)
            .add(normalizedVolatility.multiply(volatilityWeight))
            .add(normalizedConcentration.multiply(concentrationWeight))
            .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate performance score
     */
    public BigDecimal calculatePerformanceScore(BigDecimal returnPercentage, BigDecimal riskScore) {
        if (returnPercentage == null || riskScore == null) {
            return BigDecimal.ZERO;
        }

        // Simple risk-adjusted return score
        // Higher returns and lower risk = higher score
        BigDecimal normalizedReturn = normalizeReturn(returnPercentage);
        BigDecimal riskAdjustedScore = normalizedReturn.multiply(BigDecimal.ONE.subtract(riskScore));
        
        return riskAdjustedScore.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate data quality score
     */
    public BigDecimal calculateDataQualityScore(List<String> dataQualities) {
        if (dataQualities == null || dataQualities.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long highQualityCount = dataQualities.stream()
            .filter("HIGH"::equals)
            .count();
        
        long mediumQualityCount = dataQualities.stream()
            .filter("MEDIUM"::equals)
            .count();

        // Weight: HIGH = 1.0, MEDIUM = 0.7, LOW = 0.3
        BigDecimal totalScore = BigDecimal.valueOf(highQualityCount)
            .add(BigDecimal.valueOf(mediumQualityCount).multiply(new BigDecimal("0.7")))
            .add(BigDecimal.valueOf(dataQualities.size() - highQualityCount - mediumQualityCount)
                .multiply(new BigDecimal("0.3")));

        return totalScore.divide(BigDecimal.valueOf(dataQualities.size()), 4, RoundingMode.HALF_UP);
    }

    // Private helper methods for normalization

    private BigDecimal normalizeBeta(BigDecimal beta) {
        if (beta == null) return BigDecimal.ZERO;
        
        // Beta typically ranges from -2 to +3
        // Normalize to 0-1 scale where 1 = highest risk
        if (beta.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO; // Negative beta = low risk
        } else if (beta.compareTo(new BigDecimal("3")) > 0) {
            return BigDecimal.ONE; // Beta > 3 = highest risk
        } else {
            return beta.divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal normalizeVolatility(BigDecimal volatility) {
        if (volatility == null) return BigDecimal.ZERO;
        
        // Volatility typically ranges from 0% to 100%
        // Normalize to 0-1 scale
        if (volatility.compareTo(new BigDecimal("100")) > 0) {
            return BigDecimal.ONE;
        } else {
            return volatility.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal normalizeReturn(BigDecimal returnPercentage) {
        if (returnPercentage == null) return BigDecimal.ZERO;
        
        // Returns typically range from -50% to +100%
        // Normalize to 0-1 scale where 1 = highest return
        if (returnPercentage.compareTo(new BigDecimal("-50")) < 0) {
            return BigDecimal.ZERO;
        } else if (returnPercentage.compareTo(new BigDecimal("100")) > 0) {
            return BigDecimal.ONE;
        } else {
            return returnPercentage.add(new BigDecimal("50"))
                .divide(new BigDecimal("150"), 4, RoundingMode.HALF_UP);
        }
    }
}
