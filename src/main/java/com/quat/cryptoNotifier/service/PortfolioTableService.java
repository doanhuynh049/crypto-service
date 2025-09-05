package com.quat.cryptoNotifier.service;

import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service dedicated to generating comprehensive portfolio table data with enhanced features.
 * Includes sector diversification, risk assessment, technical indicators, profit/loss calculations,
 * and VND currency support.
 */
@Service
public class PortfolioTableService {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioTableService.class);
    
    // Currency conversion rates
    private static final double USD_TO_VND = 24000.0; // Approximate rate
    
    @Autowired
    private DataProviderService dataProviderService;
    /**
     * Generates comprehensive portfolio table data with enhanced features.
     * 
     * @param holdings List of portfolio holdings
     * @return Map containing portfolio data with enhanced formatting and analysis
     */
    public Map<String, Object> generatePortfolioTable(List<Holding> holdings) {
        Map<String, Object> portfolioData = new HashMap<>();
        List<Map<String, Object>> portfolioRows = new ArrayList<>();
        
        double totalInitialValue = 0;
        double totalCurrentValue = 0;
        double totalProfitLoss = 0;
        
        // Sector-based analysis maps
        Map<String, Double> sectorAllocations = new HashMap<>();
        Map<String, Double> sectorProfitLoss = new HashMap<>();
        List<Map<String, Object>> smallPositions = new ArrayList<>();

        try {
            // First pass: calculate totals for percentage calculations
            for (Holding holding : holdings) {
                totalInitialValue += holding.getTotalAvgCost();
            }

            // Process each holding
            for (Holding holding : holdings) {
                Map<String, Object> row = processHolding(holding, totalInitialValue, sectorAllocations, 
                                                       sectorProfitLoss, smallPositions);
                if (row != null) {
                    portfolioRows.add(row);
                    
                    // Accumulate totals using numeric values
                    totalCurrentValue += (Double) row.get("currentValue");
                    totalProfitLoss += (Double) row.get("profitLoss");
                }
            }

            // Generate portfolio summary
            Map<String, Object> summary = generatePortfolioSummary(totalInitialValue, totalCurrentValue, 
                                                                 totalProfitLoss, holdings.size(), 
                                                                 portfolioRows, sectorAllocations, 
                                                                 sectorProfitLoss, smallPositions);

            portfolioData.put("portfolioRows", portfolioRows);
            portfolioData.put("summary", summary);
            portfolioData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            logger.error("Error generating portfolio table", e);
            portfolioData.put("error", "Failed to generate portfolio table: " + e.getMessage());
        }

        return portfolioData;
    }

    /**
     * Process a single holding and generate its row data.
     */
    private Map<String, Object> processHolding(Holding holding, double totalInitialValue,
                                             Map<String, Double> sectorAllocations,
                                             Map<String, Double> sectorProfitLoss,
                                             List<Map<String, Object>> smallPositions) {
        try {
            Map<String, Object> row = new HashMap<>();

            // Get current market data
            MarketData marketData = dataProviderService.getMarketData(holding.getId());
            double currentPrice = marketData != null ? marketData.getCurrentPrice() : 0;

            // Basic holding information with numeric values for Thymeleaf formatting
            row.put("symbol", holding.getSymbol());
            row.put("name", holding.getName());
            row.put("sector", holding.getSector() != null ? holding.getSector() : "Unknown");
            
            // Numeric values for Thymeleaf formatting
            row.put("holdings", holding.getHoldings());
            row.put("averagePrice", holding.getAveragePrice());
            row.put("currentPrice", currentPrice);

            // Target prices as numeric values
            row.put("expectedEntry", holding.getExpectedEntry());
            row.put("deepEntryPrice", holding.getDeepEntryPrice());
            row.put("targetPrice3Month", holding.getTargetPrice3Month());
            row.put("targetPriceLongTerm", holding.getTargetPriceLongTerm());

            // Financial calculations - numeric values for Thymeleaf formatting
            double initialValue = holding.getTotalAvgCost();
            double currentValue = holding.getHoldings() * currentPrice;
            double profitLoss = currentValue - initialValue;
            double profitLossPercentage = initialValue > 0 ? (profitLoss / initialValue) * 100 : 0;

            row.put("initialValue", initialValue);
            row.put("currentValue", currentValue);
            row.put("profitLoss", profitLoss);
            row.put("profitLossPercentage", profitLossPercentage);

            // Portfolio weight percentage
            double portfolioWeight = totalInitialValue > 0 ? (initialValue / totalInitialValue) * 100 : 0;
            row.put("portfolioWeight", portfolioWeight);

            // Distance to targets as numeric values for Thymeleaf formatting
            double distanceTo3MonthTarget = currentPrice > 0 ?
                    ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100 : 0;
            double distanceToLongTarget = currentPrice > 0 ?
                    ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100 : 0;

            row.put("distanceTo3MonthTarget", distanceTo3MonthTarget);
            row.put("distanceToLongTarget", distanceToLongTarget);

            // Enhanced technical indicators and market data
            addMarketDataToRow(row, marketData, currentPrice);

            // Enhanced risk assessment for coins with >30% losses
            String riskLevel = assessRiskLevelEnhanced(holding, marketData, profitLossPercentage);
            row.put("riskLevel", riskLevel);

            // Enhanced action recommendation
            String recommendation = getActionRecommendationEnhanced(holding, marketData, profitLossPercentage, distanceTo3MonthTarget);
            row.put("recommendation", recommendation);

            // Add placeholder for AI recommendation (will be populated later by AdvisoryEngineService)
            row.put("aiRecommendation", "PENDING");
            row.put("aiRecommendationScore", 0);
            row.put("aiExplanations", Arrays.asList("AI analysis pending"));
            row.put("aiConfidence", "LOW");

            // Sector-based analysis tracking
            updateSectorAnalysis(holding, portfolioWeight, profitLoss, sectorAllocations, sectorProfitLoss);

            // Track small positions (< 2% of portfolio)
            trackSmallPositions(holding, portfolioWeight, initialValue, smallPositions);

            return row;

        } catch (Exception e) {
            logger.error("Error processing holding for {}: ", holding.getSymbol(), e);
            return null;
        }
    }

    /**
     * Add market data and technical indicators to the row.
     */
    private void addMarketDataToRow(Map<String, Object> row, MarketData marketData, double currentPrice) {
        if (marketData != null) {
            // Numeric values for Thymeleaf formatting
            row.put("priceChange24h", marketData.getPriceChangePercentage24h());
            row.put("volume24h", marketData.getVolume24h());
            row.put("marketCap", marketData.getMarketCap());
            
            // Technical indicators with numeric values for formatting
            Double rsi = marketData.getRsi();
            Double macd = marketData.getMacd();
            Double sma20 = marketData.getSma20();
            Double sma50 = marketData.getSma50();
            Double sma200 = marketData.getSma200();
            
            row.put("rsi", rsi != null ? rsi : null);
            row.put("macd", macd != null ? macd : null);
            row.put("sma20", sma20 != null ? sma20 : null);
            row.put("sma50", sma50 != null ? sma50 : null);
            row.put("sma200", sma200 != null ? sma200 : null);

            // Enhanced trend analysis
            String trend = analyzeTrendEnhanced(currentPrice, marketData);
            row.put("trend", trend);

            // Support and resistance levels as numeric values
            Map<String, Double> levels = calculateSupportResistanceLevels(marketData);
            row.put("supportLevel", levels.get("support"));
            row.put("resistanceLevel", levels.get("resistance"));
            
            // Stop-loss and take-profit calculations
            double profitLossPercentage = (Double) row.get("profitLossPercentage");
            Map<String, String> stopLossTakeProfit = calculateStopLossTakeProfit(currentPrice, marketData, profitLossPercentage);
            row.put("stopLossLevel", stopLossTakeProfit.get("stopLoss"));
            row.put("takeProfitLevel", stopLossTakeProfit.get("takeProfit"));
        } else {
            // Default values when market data is not available
            setDefaultMarketData(row);
        }
    }

    /**
     * Set default market data values when real data is not available.
     */
    private void setDefaultMarketData(Map<String, Object> row) {
        row.put("priceChange24h", 0.0);
        row.put("volume24h", 0.0);
        row.put("marketCap", 0.0);
        row.put("rsi", null);
        row.put("macd", null);
        row.put("sma20", null);
        row.put("sma50", null);
        row.put("sma200", null);
        row.put("trend", "UNKNOWN");
        row.put("supportLevel", 0.0);
        row.put("resistanceLevel", 0.0);
        row.put("stopLossLevel", "N/A");
        row.put("takeProfitLevel", "N/A");
    }

    /**
     * Enhanced trend analysis with multiple technical indicators.
     */
    private String analyzeTrendEnhanced(double currentPrice, MarketData marketData) {
        try {
            // Enhanced trend analysis with multiple indicators
            if (marketData.getSma20() != null && marketData.getSma50() != null && marketData.getSma200() != null) {
                double sma20 = marketData.getSma20();
                double sma50 = marketData.getSma50();
                double sma200 = marketData.getSma200();
                
                // Strong trends
                if (currentPrice > sma20 && sma20 > sma50 && sma50 > sma200) {
                    return "STRONG_BULLISH";
                } else if (currentPrice < sma20 && sma20 < sma50 && sma50 < sma200) {
                    return "STRONG_BEARISH";
                }
                
                // Regular trends
                if (currentPrice > sma20 && sma20 > sma50) {
                    return "BULLISH";
                } else if (currentPrice < sma20 && sma20 < sma50) {
                    return "BEARISH";
                }
            }

            // Fallback to 24h change with more granular categorization
            double change24h = marketData.getPriceChangePercentage24h();
            if (change24h > 10) {
                return "VERY_BULLISH";
            } else if (change24h > 5) {
                return "BULLISH";
            } else if (change24h > 2) {
                return "SLIGHTLY_BULLISH";
            } else if (change24h < -10) {
                return "VERY_BEARISH";
            } else if (change24h < -5) {
                return "BEARISH";
            } else if (change24h < -2) {
                return "SLIGHTLY_BEARISH";
            } else {
                return "SIDEWAYS";
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Calculate stop-loss and take-profit levels based on risk management principles.
     */
    private Map<String, String> calculateStopLossTakeProfit(double currentPrice, MarketData marketData, double profitLossPercentage) {
        Map<String, String> levels = new HashMap<>();
        
        try {
            // Stop-loss calculation based on risk management
            double stopLossLevel;
            if (profitLossPercentage > 20) {
                // If in profit, use trailing stop
                stopLossLevel = currentPrice * 0.85; // 15% trailing stop
            } else if (profitLossPercentage < -20) {
                // If in significant loss, tighter stop
                stopLossLevel = currentPrice * 0.95; // 5% stop
            } else {
                // Standard stop-loss
                stopLossLevel = currentPrice * 0.90; // 10% stop
            }
            
            // Take-profit calculation
            double takeProfitLevel;
            if (profitLossPercentage > 50) {
                // Already in significant profit, conservative take-profit
                takeProfitLevel = currentPrice * 1.10; // 10% above current
            } else {
                // Standard take-profit
                takeProfitLevel = currentPrice * 1.25; // 25% above current
            }
            
            levels.put("stopLoss", String.format("$%.4f", stopLossLevel));
            levels.put("takeProfit", String.format("$%.4f", takeProfitLevel));
            
        } catch (Exception e) {
            levels.put("stopLoss", "N/A");
            levels.put("takeProfit", "N/A");
        }
        
        return levels;
    }

    /**
     * Enhanced risk assessment with multiple factors including >30% loss handling.
     */
    private String assessRiskLevelEnhanced(Holding holding, MarketData marketData, double profitLossPercentage) {
        try {
            int riskScore = 0;

            // Enhanced RSI-based risk assessment
            if (marketData != null && marketData.getRsi() != null) {
                double rsi = marketData.getRsi();
                if (rsi > 80) riskScore += 3; // Extremely overbought
                else if (rsi > 70) riskScore += 2; // Overbought
                else if (rsi < 20) riskScore += 3; // Extremely oversold
                else if (rsi < 30) riskScore += 2; // Oversold
            }

            // 24h change based risk with enhanced thresholds
            if (marketData != null) {
                double change = Math.abs(marketData.getPriceChangePercentage24h());
                if (change > 30) riskScore += 4; // Extreme volatility
                else if (change > 20) riskScore += 3; // Very high volatility
                else if (change > 10) riskScore += 2; // High volatility
                else if (change > 5) riskScore += 1; // Moderate volatility
            }

            // Position value risk (higher value = higher risk)
            double positionValue = holding.getTotalAvgCost();
            if (positionValue > 100000) riskScore += 3; // Very large position
            else if (positionValue > 50000) riskScore += 2; // Large position
            else if (positionValue > 20000) riskScore += 1; // Medium position

            // Enhanced loss-based risk assessment (>30% losses get special attention)
            if (profitLossPercentage < -30) {
                riskScore += 4; // Critical loss level
            } else if (profitLossPercentage < -20) {
                riskScore += 2; // Significant loss
            } else if (profitLossPercentage < -10) {
                riskScore += 1; // Moderate loss
            }

            // Sector-based risk assessment
            riskScore += calculateSectorRisk(holding.getSector());

            // Final risk categorization
            if (riskScore >= 8) return "CRITICAL";
            else if (riskScore >= 6) return "VERY_HIGH";
            else if (riskScore >= 4) return "HIGH";
            else if (riskScore >= 2) return "MEDIUM";
            else return "LOW";

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Calculate risk score based on sector characteristics.
     */
    private int calculateSectorRisk(String sector) {
        if (sector == null) return 0;
        
        switch (sector.toLowerCase()) {
            case "ai/ml":
            case "defi":
                return 1; // Higher volatility sectors
            case "stablecoin":
                return -2; // Lower risk
            case "layer 1":
                return 0; // Neutral risk
            default:
                return 0;
        }
    }

    /**
     * Enhanced action recommendation with comprehensive analysis.
     */
    private String getActionRecommendationEnhanced(Holding holding, MarketData marketData, double profitLossPercentage, double distanceTo3MonthTarget) {
        try {
            // Critical loss management (>30% loss)
            if (profitLossPercentage < -30) {
                return "URGENT_REVIEW_CONSIDER_EXIT";
            } else if (profitLossPercentage < -20) {
                return "CONSIDER_STOP_LOSS";
            }
            
            // Profit-taking recommendations
            if (profitLossPercentage > 100) {
                return "TAKE_MAJOR_PROFIT";
            } else if (profitLossPercentage > 50) {
                return "TAKE_PARTIAL_PROFIT";
            } else if (profitLossPercentage > 20) {
                return "HOLD_STRONG_CONSIDER_TRIM";
            }
            
            // Target-based recommendations
            if (distanceTo3MonthTarget < 5) {
                return "NEAR_TARGET_CONSIDER_SELL";
            } else if (distanceTo3MonthTarget > 50) {
                return "FAR_FROM_TARGET_MONITOR";
            }

            // RSI-based recommendations
            if (marketData != null && marketData.getRsi() != null) {
                double rsi = marketData.getRsi();
                if (rsi > 80) {
                    return "OVERBOUGHT_WAIT_FOR_DIP";
                } else if (rsi < 30) {
                    return "OVERSOLD_CONSIDER_BUYING";
                } else if (rsi < 20) {
                    return "EXTREMELY_OVERSOLD_STRONG_BUY";
                }
            }
            
            return "HOLD";
        } catch (Exception e) {
            return "REVIEW";
        }
    }

    /**
     * Calculate support and resistance levels using technical analysis.
     */
    private Map<String, Double> calculateSupportResistanceLevels(MarketData marketData) {
        Map<String, Double> levels = new HashMap<>();
        try {
            double currentPrice = marketData.getCurrentPrice();

            // Simple support/resistance calculation based on SMAs
            if (marketData.getSma20() != null && marketData.getSma50() != null) {
                double support = Math.min(marketData.getSma20(), marketData.getSma50());
                double resistance = Math.max(marketData.getSma20(), marketData.getSma50());

                // Adjust based on current price position
                if (currentPrice < support) {
                    resistance = support;
                    support = currentPrice * 0.95; // 5% below current
                } else if (currentPrice > resistance) {
                    support = resistance;
                    resistance = currentPrice * 1.05; // 5% above current
                }

                levels.put("support", support);
                levels.put("resistance", resistance);
            } else {
                // Fallback calculation
                levels.put("support", currentPrice * 0.95);
                levels.put("resistance", currentPrice * 1.05);
            }
        } catch (Exception e) {
            levels.put("support", 0.0);
            levels.put("resistance", 0.0);
        }

        return levels;
    }

    /**
     * Update sector-based analysis tracking.
     */
    private void updateSectorAnalysis(Holding holding, double portfolioWeight, double profitLoss,
                                    Map<String, Double> sectorAllocations, Map<String, Double> sectorProfitLoss) {
        String sector = holding.getSector() != null ? holding.getSector() : "Unknown";
        sectorAllocations.put(sector, sectorAllocations.getOrDefault(sector, 0.0) + portfolioWeight);
        sectorProfitLoss.put(sector, sectorProfitLoss.getOrDefault(sector, 0.0) + profitLoss);
    }

    /**
     * Track small positions for consolidation recommendations.
     */
    private void trackSmallPositions(Holding holding, double portfolioWeight, double initialValue,
                                   List<Map<String, Object>> smallPositions) {
        // Identify small positions (< 2% of portfolio)
        if (portfolioWeight < 2.0) {
            Map<String, Object> smallPosition = new HashMap<>();
            smallPosition.put("symbol", holding.getSymbol());
            smallPosition.put("weight", String.format("%.1f%%", portfolioWeight));
            smallPosition.put("value", String.format("$%.2f", initialValue));
            smallPositions.add(smallPosition);
        }
    }

    /**
     * Generate comprehensive portfolio summary with VND support.
     */
    private Map<String, Object> generatePortfolioSummary(double totalInitialValue, double totalCurrentValue,
                                                        double totalProfitLoss, int numberOfHoldings,
                                                        List<Map<String, Object>> portfolioRows,
                                                        Map<String, Double> sectorAllocations,
                                                        Map<String, Double> sectorProfitLoss,
                                                        List<Map<String, Object>> smallPositions) {
        Map<String, Object> summary = new HashMap<>();

        // Portfolio totals - provide raw numeric values for Thymeleaf formatting
        double totalProfitLossPercentage = totalInitialValue > 0 ? (totalProfitLoss / totalInitialValue) * 100 : 0;

        // Raw numeric values for Thymeleaf formatting
        summary.put("totalInitialValue", totalInitialValue);
        summary.put("totalCurrentValue", totalCurrentValue);
        summary.put("totalProfitLoss", totalProfitLoss);
        summary.put("totalProfitLossPercentage", totalProfitLossPercentage);
        
        // Formatted strings for direct display (if needed elsewhere)
        summary.put("totalInitialValueFormatted", String.format("$%.2f", totalInitialValue));
        summary.put("totalCurrentValueFormatted", String.format("$%.2f", totalCurrentValue));
        summary.put("totalProfitLossFormatted", String.format("$%.2f", totalProfitLoss));
        summary.put("totalProfitLossPercentageFormatted", String.format("%.2f%%", totalProfitLossPercentage));
        
        // VND currency totals with comma formatting
        summary.put("totalInitialValueVND", String.format("₫%,.0f", totalInitialValue * USD_TO_VND));
        summary.put("totalCurrentValueVND", String.format("₫%,.0f", totalCurrentValue * USD_TO_VND));
        summary.put("totalProfitLossVND", String.format("₫%,.0f", totalProfitLoss * USD_TO_VND));
        
        summary.put("numberOfHoldings", numberOfHoldings);

        // Risk distribution analysis
        Map<String, Integer> riskDistribution = calculateRiskDistribution(portfolioRows);
        summary.put("riskDistribution", riskDistribution);
        
        // Sector diversification analysis
        Map<String, Object> sectorAnalysis = createSectorAnalysis(sectorAllocations, sectorProfitLoss);
        summary.put("sectorAnalysis", sectorAnalysis);
        
        // Small position consolidation recommendations
        if (!smallPositions.isEmpty()) {
            summary.put("smallPositions", smallPositions);
            summary.put("consolidationRecommendation", generateConsolidationRecommendation(smallPositions));
        }

        return summary;
    }

    /**
     * Create comprehensive sector analysis.
     */
    private Map<String, Object> createSectorAnalysis(Map<String, Double> sectorAllocations, Map<String, Double> sectorProfitLoss) {
        Map<String, Object> sectorAnalysis = new HashMap<>();
        sectorAnalysis.put("allocations", sectorAllocations);
        sectorAnalysis.put("profitLoss", sectorProfitLoss);
        sectorAnalysis.put("diversificationScore", calculateDiversificationScore(sectorAllocations));
        return sectorAnalysis;
    }

    /**
     * Calculate portfolio diversification score using Herfindahl-Hirschman Index.
     */
    private String calculateDiversificationScore(Map<String, Double> sectorAllocations) {
        try {
            // Calculate Herfindahl-Hirschman Index for diversification
            double hhi = 0;
            for (double allocation : sectorAllocations.values()) {
                hhi += Math.pow(allocation / 100, 2);
            }
            
            // Convert to diversification score
            if (hhi < 0.15) return "EXCELLENT";
            else if (hhi < 0.25) return "GOOD";
            else if (hhi < 0.40) return "FAIR";
            else return "POOR";
            
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Generate small position consolidation recommendation.
     */
    private String generateConsolidationRecommendation(List<Map<String, Object>> smallPositions) {
        if (smallPositions.size() >= 5) {
            return "CONSIDER_CONSOLIDATING: " + smallPositions.size() + " positions under 2% could be consolidated into larger, more impactful positions";
        } else if (smallPositions.size() >= 3) {
            return "MONITOR: " + smallPositions.size() + " small positions may benefit from consolidation";
        } else {
            return "ACCEPTABLE: Small position count is manageable";
        }
    }

    /**
     * Calculate risk distribution across the portfolio.
     */
    private Map<String, Integer> calculateRiskDistribution(List<Map<String, Object>> portfolioRows) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("LOW", 0);
        distribution.put("MEDIUM", 0);
        distribution.put("HIGH", 0);
        distribution.put("VERY_HIGH", 0);
        distribution.put("CRITICAL", 0);
        distribution.put("UNKNOWN", 0);

        for (Map<String, Object> row : portfolioRows) {
            String risk = (String) row.get("riskLevel");
            if (risk != null) {
                distribution.put(risk, distribution.getOrDefault(risk, 0) + 1);
            }
        }

        return distribution;
    }

}
