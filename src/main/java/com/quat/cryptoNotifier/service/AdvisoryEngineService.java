package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.config.AppConfig;
import com.quat.cryptoNotifier.model.Advisory;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.MarketData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AdvisoryEngineService {

    private static final Logger logger = LoggerFactory.getLogger(AdvisoryEngineService.class);
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private DataProviderService dataProviderService;

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private RiskOpportunityAnalysisService riskOpportunityAnalysisService;
    
    @Autowired
    private OpportunityFinderAnalysisService opportunityFinderAnalysisService;
    
    @Autowired
    private EntryExitStrategyAnalysisService entryExitStrategyAnalysisService;
    
    @Autowired
    private PortfolioOptimizationAnalysisService portfolioOptimizationAnalysisService;
    
    @Autowired
    private PortfolioHealthCheckAnalysisService portfolioHealthCheckAnalysisService;

    @Autowired
    private USDTAllocationAnalysisService usdtAllocationAnalysisService;

    @Autowired
    private InvestmentAnalysisService investmentAnalysisService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AdvisoryEngineService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    public String generateOverviewHoldingAdvisories(List<Holding> holdings) {
        String prompt = buildPromptForOverview(holdings);
        System.out.println("Overview Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Overview AI Response: " + aiResponse);
        return aiResponse;
    }

    public Advisory generateAdvisory(Holding holding, MarketData marketData) {
        try {
            Advisory advisory = new Advisory(holding.getSymbol());
            
            // Calculate financial metrics
            double currentPrice = marketData.getCurrentPrice();
            double profitLoss = holding.getProfitLoss(currentPrice);
            double profitLossPercentage = holding.getProfitLossPercentage(currentPrice);
            double percentageToTarget = holding.getPercentageToTarget(currentPrice);

            advisory.setCurrentPrice(currentPrice);
            advisory.setProfitLoss(profitLoss);
            advisory.setProfitLossPercentage(profitLossPercentage);
            advisory.setPercentageToTarget(percentageToTarget);
            advisory.setRsi(marketData.getRsi());
            advisory.setMacd(marketData.getMacd());
            advisory.setSma20(marketData.getSma20());
            advisory.setSma50(marketData.getSma50());
            advisory.setSma200(marketData.getSma200());

            // Generate AI advisory
            String prompt = buildPrompt(holding, marketData, advisory);
            String aiResponse = callGeminiAPI(prompt);
            
            // Parse AI response
            parseAIResponse(advisory, aiResponse);

            return advisory;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate advisory for " + holding.getSymbol(), e);
        }
    }

    public Map<String, Object> generateRiskOpportunityAnalysis(List<Holding> holdings) {
        String prompt = riskOpportunityAnalysisService.buildRiskOpportunityPrompt(holdings);
        System.out.println("Risk & Opportunity Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Risk & Opportunity Analysis AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = riskOpportunityAnalysisService.parseRiskOpportunityResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generatePortfolioHealthCheck(List<Holding> holdings) {
        String prompt = portfolioHealthCheckAnalysisService.buildPortfolioHealthCheckPrompt(holdings);
        System.out.println("Portfolio Health Check Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Portfolio Health Check AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = portfolioHealthCheckAnalysisService.parsePortfolioHealthCheckResponse(aiResponse);
        return parsedAnalysis;
    }
    

    public Map<String, Object> generateOpportunityFinder(List<Holding> holdings) {
        String prompt = opportunityFinderAnalysisService.buildOpportunityFinderPrompt(holdings);
        System.out.println("Opportunity Finder Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Opportunity Finder AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = opportunityFinderAnalysisService.parseOpportunityFinderResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generatePortfolioOptimizationAnalysis(List<Holding> holdings) {
        String prompt = portfolioOptimizationAnalysisService.buildPortfolioOptimizationPrompt(holdings);
        System.out.println("Portfolio Optimization Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Portfolio Optimization Analysis AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = portfolioOptimizationAnalysisService.parsePortfolioOptimizationResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generateEntryExitStrategy(List<Holding> holdings) {
        String prompt = entryExitStrategyAnalysisService.buildEntryExitStrategyPrompt(holdings);
        System.out.println("Entry & Exit Strategy Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Entry & Exit Strategy Analysis AI Response: " + aiResponse);

        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = entryExitStrategyAnalysisService.parseEntryExitStrategyResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generateUSDTAllocationStrategy(List<Holding> holdings) {
        double usdtAmount = holdings.stream()
                .filter(h -> h.getSymbol().equalsIgnoreCase("USDT"))
                .findFirst()
                .map(Holding::getHoldings)
                .orElse(0.0);

        String prompt = usdtAllocationAnalysisService.buildUSDTAllocationPrompt(holdings, usdtAmount);
        System.out.println("USDT Allocation Strategy Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("USDT Allocation Strategy AI Response: " + aiResponse);

        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = usdtAllocationAnalysisService.parseUSDTAllocationResponse(aiResponse);
        return parsedAnalysis;
    }

    //=============================================================================================

    private String buildPromptForOverview(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a crypto investment advisor. Analyze the following portfolio and provide structured advice.\n\n");
        prompt.append(investmentStrategyService.getPortfolioTargetsSection());
        for (Holding holding : holdings) {
            prompt.append(String.format("Symbol: %s\n", holding.getSymbol()));
            prompt.append(String.format("Amount: %.4f\n", holding.getHoldings()));
            prompt.append(String.format("Average Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append("\n");
        }
        // --- Analysis Requirements ---
        prompt.append("Please provide insights on the following:\n\n");
        prompt.append("1. **Risk Assessment**: Evaluate the portfolio’s overall risk profile considering asset class exposure (Layer 1s, DeFi, etc.), volatility, and diversification. Highlight the main risk factors.\n");
        prompt.append("2. **Risk vs. Reward Balance**: Assess how well the portfolio balances upside potential with downside protection.\n");
        prompt.append("3. **Weighting Analysis**: Identify which assets may be overweighted or underweighted relative to fundamentals, growth outlook, and risks.\n");
        prompt.append("4. **Actionable Risk Reduction**: Suggest practical steps to lower risk, such as rebalancing allocations, diversifying into stablecoins, staking opportunities, or defensive assets.\n");
        prompt.append("5. **Opportunity Maximization**: Recommend areas/sectors where increasing exposure could enhance long-term growth (e.g., AI, DeFi, Web3 infra, interoperability).\n");
        prompt.append("6. **Missing Pieces**: Point out any missing assets or sectors that could strengthen diversification and resilience.\n\n");

        // --- Context & Preferences ---
        prompt.append("⚠️ Context & Preferences:\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());

        // --- Output Format ---
        prompt.append("👉 Please provide a balanced, data-driven analysis in the following structured JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"portfolio_action\": \"HOLD|REBALANCE|TRIM|ADD\",\n");
        prompt.append("  \"summary\": \"Brief overview of portfolio health and key risks\",\n");
        prompt.append("  \"risk_assessment\": \"Main risk factors and exposure insights\",\n");
        prompt.append("  \"allocation_suggestions\": [\n");
        prompt.append("    { \"symbol\": \"BTC\", \"action\": \"HOLD|TRIM|ADD\", \"new_allocation\": \"20%\", \"rationale\": \"Reasoning here\" }\n");
        prompt.append("  ],\n");
        prompt.append("  \"opportunity_sectors\": [\"AI\", \"DeFi\", \"Web3 infra\", \"Interoperability\"],\n");
        prompt.append("  \"missing_assets\": [\"Stablecoins\", \"Staking tokens\"],\n");
        prompt.append("  \"risk_notes\": \"Key risk considerations to monitor\"\n");
        prompt.append("}\n");
        return prompt.toString();
    }

    private String buildPrompt(Holding holding, MarketData marketData, Advisory advisory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a crypto investment advisor. Analyze the following position and provide structured advice.\n\n");
        
        prompt.append("POSITION DETAILS:\n");
        prompt.append(String.format("Symbol: %s\n", holding.getSymbol()));
        prompt.append(String.format("Amount: %.4f\n", holding.getHoldings()));
        prompt.append(String.format("Average Price: $%.2f\n", holding.getAveragePrice()));
        prompt.append(String.format("Target Price (3M): $%.2f\n", holding.getTargetPrice3Month()));
        prompt.append(String.format("Target Price (Long): $%.2f\n", holding.getTargetPriceLongTerm()));
        
        prompt.append("\nCURRENT MARKET DATA:\n");
        prompt.append(String.format("Current Price: $%.2f\n", marketData.getCurrentPrice()));
        prompt.append(String.format("24h Change: %.2f%%\n", marketData.getPriceChangePercentage24h()));
        prompt.append(String.format("P/L: $%.2f (%.2f%%)\n", advisory.getProfitLoss(), advisory.getProfitLossPercentage()));
        prompt.append(String.format("Distance to Target: %.2f%%\n", advisory.getPercentageToTarget()));
        
        prompt.append("\nTECHNICAL INDICATORS:\n");
        if (marketData.getRsi() != null) {
            prompt.append(String.format("RSI (14): %.1f\n", marketData.getRsi()));
        }
        if (marketData.getMacd() != null) {
            prompt.append(String.format("MACD: %.4f\n", marketData.getMacd()));
        }
        if (marketData.getSma20() != null) {
            prompt.append(String.format("SMA 20: $%.2f\n", marketData.getSma20()));
        }
        if (marketData.getSma50() != null) {
            prompt.append(String.format("SMA 50: $%.2f\n", marketData.getSma50()));
        }
        if (marketData.getSma200() != null) {
            prompt.append(String.format("SMA 200: $%.2f\n", marketData.getSma200()));
        }
        
        prompt.append("\nProvide your analysis in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"action\": \"HOLD|BUY|SELL|TAKE_PROFIT\",\n");
        prompt.append("  \"rationale\": \"Brief explanation of the recommendation\",\n");
        prompt.append("  \"levels\": \"Key support/resistance levels to watch\",\n");
        prompt.append("  \"risk_notes\": \"Important risk considerations\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private String callGeminiAPI(String prompt) {
        // First attempt with primary provider
        try {
            return callGeminiAPIWithProvider(prompt, appConfig.getLlmProvider(), 1);
        } catch (Exception e) {
            logger.warn("First attempt failed with primary provider: {}", e.getMessage());
            
            // First retry with primary provider
            try {
                Thread.sleep(1000); // Wait 1 second before retry
                return callGeminiAPIWithProvider(prompt, appConfig.getLlmProvider(), 2);
            } catch (Exception e2) {
                logger.warn("Second attempt failed with primary provider: {}", e2.getMessage());
                
                // Second retry with old provider
                try {
                    Thread.sleep(1000); // Wait 1 second before retry
                    return callGeminiAPIWithProvider(prompt, appConfig.getLlmProviderOld(), 3);
                } catch (Exception e3) {
                    logger.error("All attempts failed. Final attempt with old provider failed: {}", e3.getMessage());
                    return "{}";
                }
            }
        }
    }

    private String callGeminiAPIWithProvider(String prompt, String providerUrl, int attemptNumber) throws Exception {
        logger.info("Attempting Gemini API call #{} with provider: {}", attemptNumber, providerUrl);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", appConfig.getLlmApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", new Object[]{part});
        requestBody.put("contents", new Object[]{content});

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.exchange(
                providerUrl,
                HttpMethod.POST,
                request,
                String.class
            ).getBody();

            // Extract text from Gemini response
            JsonNode responseNode = objectMapper.readTree(response);
            JsonNode candidates = responseNode.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content1 = candidates.get(0).get("content");
                if (content1 != null) {
                    JsonNode parts = content1.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        logger.info("Successfully received response from attempt #{}", attemptNumber);
                        return parts.get(0).get("text").asText();
                    }
                }
            }

            return "{}"; // Return empty JSON if parsing fails

        } catch (Exception e) {
            // Check if it's a 503 Service Unavailable error that indicates overload
            if (e.getMessage() != null && (
                e.getMessage().contains("503") || 
                e.getMessage().contains("Service Unavailable") || 
                e.getMessage().contains("overloaded") ||
                e.getMessage().contains("UNAVAILABLE"))) {
                
                logger.warn("Provider overloaded (503 error) on attempt #{}: {}", attemptNumber, e.getMessage());
                throw new RuntimeException("Provider overloaded: " + e.getMessage(), e);
            }
            
            // For other errors, still throw to trigger retry
            logger.error("API call failed on attempt #{}: {}", attemptNumber, e.getMessage());
            throw e;
        }
    }

    private void parseAIResponse(Advisory advisory, String response) {
        try {
            // Extract JSON from response (in case there's extra text)
            String jsonResponse = response;
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse = response.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode responseNode = objectMapper.readTree(jsonResponse);

            if (responseNode.has("action")) {
                advisory.setAction(responseNode.get("action").asText());
            }
            if (responseNode.has("rationale")) {
                advisory.setRationale(responseNode.get("rationale").asText());
            }
            if (responseNode.has("levels")) {
                advisory.setLevels(responseNode.get("levels").asText());
            }
            if (responseNode.has("risk_notes")) {
                advisory.setRiskNotes(responseNode.get("risk_notes").asText());
            }

        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            // Set default values if parsing fails
            advisory.setAction("HOLD");
            advisory.setRationale("Unable to generate advisory due to API response parsing error");
            advisory.setLevels("Monitor key technical levels");
            advisory.setRiskNotes("Exercise caution due to analysis limitations");
        }
    }

    // Investment Analysis Methods
    public Map<String, Object> generateInvestmentAnalysis(Holding holding) {
        try {
            // Fetch market data for the specified crypto using DataProviderService
            MarketData marketData = dataProviderService.getMarketData(holding.getId());
            
            // Convert MarketData to Map for compatibility with prompt builder
            Map<String, Object> marketDataMap = convertMarketDataToMap(marketData);
            
            // Build prompt for comprehensive investment analysis using dedicated service
            String prompt = investmentAnalysisService.buildInvestmentAnalysisPrompt(holding, marketDataMap);
            
            // Get AI analysis using Gemini API
            String aiResponse = callGeminiAPI(prompt);
            
            // Parse and structure the response using dedicated service
            return investmentAnalysisService.parseInvestmentAnalysisResponse(aiResponse, holding.getSymbol());
            
        } catch (Exception e) {
            logger.error("Error generating investment analysis for {}: ", holding.getSymbol(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate investment analysis for " + holding.getSymbol());
            errorResponse.put("bottom_line", "WAIT - Unable to analyze current market conditions");
            errorResponse.put("recommendation", "WAIT");
            errorResponse.put("confidence", "LOW");
            errorResponse.put("symbol", holding.getSymbol());
            errorResponse.put("name", holding.getName());
            return errorResponse;
        }
    }
    
    // Legacy method for ETH analysis - calls the generic method
    public Map<String, Object> generateETHInvestmentAnalysis() {
        Holding ethHolding = new Holding();
        ethHolding.setSymbol("ETH");
        ethHolding.setName("Ethereum");
        return generateInvestmentAnalysis(ethHolding);
    }
    
    private Map<String, Object> convertMarketDataToMap(MarketData marketData) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("price", marketData.getCurrentPrice());
        dataMap.put("priceChange24h", marketData.getPriceChangePercentage24h());
        dataMap.put("priceChange7d", 0.0); // Not available in MarketData, set default
        dataMap.put("marketCap", 0.0); // Not available in MarketData, set default
        dataMap.put("volume24h", marketData.getVolume24h());
        return dataMap;
    }

    public Map<String, Object> generatePortfolioTable(List<Holding> holdings) {
        Map<String, Object> portfolioData = new HashMap<>();
        List<Map<String, Object>> portfolioRows = new ArrayList<>();

        double totalInitialValue = 0;
        double totalCurrentValue = 0;
        double totalProfitLoss = 0;

        try {
            for (Holding holding : holdings) {
                Map<String, Object> row = new HashMap<>();

                // Get current market data
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                double currentPrice = marketData != null ? marketData.getCurrentPrice() : 0;

                // Basic holding information
                row.put("symbol", holding.getSymbol());
                row.put("name", holding.getName());
                row.put("holdings", holding.getHoldings());
                row.put("averagePrice", holding.getAveragePrice());
                row.put("currentPrice", currentPrice);

                // Target prices
                row.put("expectedEntry", holding.getExpectedEntry());
                row.put("deepEntryPrice", holding.getDeepEntryPrice());
                row.put("targetPrice3Month", holding.getTargetPrice3Month());
                row.put("targetPriceLongTerm", holding.getTargetPriceLongTerm());

                // Financial calculations
                double initialValue = holding.getTotalAvgCost();
                double currentValue = holding.getHoldings() * currentPrice;
                double profitLoss = currentValue - initialValue;
                double profitLossPercentage = initialValue > 0 ? (profitLoss / initialValue) * 100 : 0;

                row.put("initialValue", initialValue);
                row.put("currentValue", currentValue);
                row.put("profitLoss", profitLoss);
                row.put("profitLossPercentage", profitLossPercentage);

                // Distance to targets
                double distanceTo3MonthTarget = currentPrice > 0 ?
                        ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100 : 0;
                double distanceToLongTarget = currentPrice > 0 ?
                        ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100 : 0;

                row.put("distanceTo3MonthTarget", distanceTo3MonthTarget);
                row.put("distanceToLongTarget", distanceToLongTarget);

                // Market data and technical indicators
                if (marketData != null) {
                    row.put("priceChange24h", marketData.getPriceChangePercentage24h());
                    row.put("volume24h", marketData.getVolume24h());
                    row.put("marketCap", marketData.getMarketCap());
                    row.put("rsi", marketData.getRsi());
                    row.put("macd", marketData.getMacd());
                    row.put("sma20", marketData.getSma20());
                    row.put("sma50", marketData.getSma50());
                    row.put("sma200", marketData.getSma200());

                    // Trend analysis
                    String trend = analyzeTrend(currentPrice, marketData);
                    row.put("trend", trend);

                    // Support and resistance levels
                    Map<String, Double> levels = calculateSupportResistanceLevels(marketData);
                    row.put("supportLevel", levels.get("support"));
                    row.put("resistanceLevel", levels.get("resistance"));
                } else {
                    // Default values when market data is not available
                    row.put("priceChange24h", 0.0);
                    row.put("volume24h", 0.0);
                    row.put("marketCap", 0.0);
                    row.put("trend", "UNKNOWN");
                    row.put("supportLevel", 0.0);
                    row.put("resistanceLevel", 0.0);
                }

                // Portfolio weight
                double portfolioWeight = totalInitialValue > 0 ? (initialValue / getTotalPortfolioValue(holdings)) * 100 : 0;
                row.put("portfolioWeight", portfolioWeight);

                // Risk assessment
                String riskLevel = assessRiskLevel(holding, marketData);
                row.put("riskLevel", riskLevel);

                // Action recommendation
                String recommendation = getActionRecommendation(holding, marketData, profitLossPercentage);
                row.put("recommendation", recommendation);

                portfolioRows.add(row);

                // Accumulate totals
                totalInitialValue += initialValue;
                totalCurrentValue += currentValue;
                totalProfitLoss += profitLoss;
            }

            // Portfolio summary
            double totalProfitLossPercentage = totalInitialValue > 0 ? (totalProfitLoss / totalInitialValue) * 100 : 0;

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalInitialValue", totalInitialValue);
            summary.put("totalCurrentValue", totalCurrentValue);
            summary.put("totalProfitLoss", totalProfitLoss);
            summary.put("totalProfitLossPercentage", totalProfitLossPercentage);
            summary.put("numberOfHoldings", holdings.size());

            // Risk distribution
            Map<String, Integer> riskDistribution = calculateRiskDistribution(portfolioRows);
            summary.put("riskDistribution", riskDistribution);

            portfolioData.put("portfolioRows", portfolioRows);
            portfolioData.put("summary", summary);
            portfolioData.put("timestamp", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            logger.error("Error generating portfolio table", e);
            portfolioData.put("error", "Failed to generate portfolio table: " + e.getMessage());
        }

        return portfolioData;
    }


    private Map<String, Integer> calculateRiskDistribution(List<Map<String, Object>> portfolioRows) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("LOW", 0);
        distribution.put("MEDIUM", 0);
        distribution.put("HIGH", 0);
        distribution.put("UNKNOWN", 0);

        for (Map<String, Object> row : portfolioRows) {
            String risk = (String) row.get("riskLevel");
            distribution.put(risk, distribution.get(risk) + 1);
        }

        return distribution;
    }



    private String analyzeTrend(double currentPrice, MarketData marketData) {
        try {
            if (marketData.getSma20() != null && marketData.getSma50() != null) {
                if (currentPrice > marketData.getSma20() && marketData.getSma20() > marketData.getSma50()) {
                    return "BULLISH";
                } else if (currentPrice < marketData.getSma20() && marketData.getSma20() < marketData.getSma50()) {
                    return "BEARISH";
                } else {
                    return "SIDEWAYS";
                }
            }

            // Fallback to 24h change
            if (marketData.getPriceChangePercentage24h() > 5) {
                return "BULLISH";
            } else if (marketData.getPriceChangePercentage24h() < -5) {
                return "BEARISH";
            } else {
                return "SIDEWAYS";
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

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

    private String assessRiskLevel(Holding holding, MarketData marketData) {
        try {
            // Risk assessment based on volatility, RSI, and position size
            int riskScore = 0;

            // RSI-based risk
            if (marketData != null && marketData.getRsi() != null) {
                if (marketData.getRsi() > 80) riskScore += 2; // Overbought
                else if (marketData.getRsi() > 70) riskScore += 1;
                else if (marketData.getRsi() < 20) riskScore += 2; // Oversold
                else if (marketData.getRsi() < 30) riskScore += 1;
            }

            // 24h change based risk
            if (marketData != null) {
                double change = Math.abs(marketData.getPriceChangePercentage24h());
                if (change > 20) riskScore += 3;
                else if (change > 10) riskScore += 2;
                else if (change > 5) riskScore += 1;
            }

            // Position value risk (higher value = higher risk)
            double positionValue = holding.getTotalAvgCost();
            if (positionValue > 50000) riskScore += 2;
            else if (positionValue > 20000) riskScore += 1;

            if (riskScore >= 5) return "HIGH";
            else if (riskScore >= 3) return "MEDIUM";
            else return "LOW";

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String getActionRecommendation(Holding holding, MarketData marketData, double profitLossPercentage) {
        try {
            // Simple recommendation logic
            if (profitLossPercentage > 50) {
                return "TAKE_PARTIAL_PROFIT";
            } else if (profitLossPercentage > 20) {
                return "HOLD_STRONG";
            } else if (profitLossPercentage < -20) {
                return "CONSIDER_AVERAGING_DOWN";
            } else if (marketData != null && marketData.getRsi() != null) {
                if (marketData.getRsi() > 80) {
                    return "WAIT_FOR_DIP";
                } else if (marketData.getRsi() < 30) {
                    return "CONSIDER_BUYING";
                }
            }
            return "HOLD";
        } catch (Exception e) {
            return "REVIEW";
        }
    }
    private double getTotalPortfolioValue(List<Holding> holdings) {
        return holdings.stream()
                .mapToDouble(Holding::getTotalAvgCost)
                .sum();
    }
}
