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
    private AdvisorAIService advisorAIService;

    @Autowired
    private InvestmentAnalysisService investmentAnalysisService;

    @Autowired
    private PortfolioTableService portfolioTableService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AdvisoryEngineService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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

    public Map<String, Object> generateInvestmentStrategyAnalysis(List<Holding> holdings) {
        String prompt = advisorAIService.buildInvestmentStrategyPrompt(holdings);
        System.out.println("Investment Strategy Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Investment Strategy Analysis AI Response: " + aiResponse);

        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = advisorAIService.parseInvestmentStrategyResponse(aiResponse);
        return parsedAnalysis;
    }

    //=============================================================================================

    public String callGeminiAPI(String prompt) {
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

    /**
     * Generates comprehensive portfolio table data.
     * This method delegates to the dedicated PortfolioTableService for enhanced maintainability.
     * 
     * @param holdings List of portfolio holdings
     * @return Map containing portfolio data with comprehensive analysis
     */
    public Map<String, Object> generatePortfolioTable(List<Holding> holdings) {
        // First get the basic portfolio data without AI recommendations
        Map<String, Object> portfolioData = portfolioTableService.generatePortfolioTable(holdings);
        
        // Then add AI recommendations to each row
        addAIRecommendationsToPortfolioData(portfolioData);
        
        return portfolioData;
    }

    /**
     * Add AI recommendations to portfolio data rows.
     */
    @SuppressWarnings("unchecked")
    private void addAIRecommendationsToPortfolioData(Map<String, Object> portfolioData) {
        try {
            List<Map<String, Object>> portfolioRows = (List<Map<String, Object>>) portfolioData.get("portfolioRows");
            
            if (portfolioRows != null) {
                for (Map<String, Object> row : portfolioRows) {
                    try {
                        // Prepare technical parameters for AI analysis
                        Map<String, Object> technicalParams = prepareTechnicalParametersFromRow(row);
                        
                        // Get AI recommendation with explanations
                        Map<String, Object> aiResponse = getAIRecommendationWithExplanations(technicalParams);
                        
                        // Update the row with AI recommendation data
                        row.put("aiRecommendation", aiResponse.get("recommendation"));
                        row.put("aiRecommendationScore", aiResponse.get("confidence")); // Use confidence as score
                        
                        // Ensure explanations is always a List, never null
                        Object explanations = aiResponse.get("explanations");
                        if (explanations == null) {
                            explanations = java.util.Arrays.asList("No explanation available");
                        }
                        row.put("aiExplanations", explanations);
                        
                        // Convert numeric confidence to confidence level
                        int confidenceScore = (Integer) aiResponse.get("confidence");
                        String confidenceLevel = convertConfidenceToLevel(confidenceScore);
                        row.put("aiConfidence", confidenceLevel);
                        
                    } catch (Exception e) {
                        logger.error("Failed to fetch AI recommendation for {}: ", row.get("symbol"), e);
                        row.put("aiRecommendation", "AI_UNAVAILABLE");
                        row.put("aiRecommendationScore", 0);
                        row.put("aiExplanations", java.util.Arrays.asList("AI recommendation service temporarily unavailable"));
                        row.put("aiConfidence", "LOW");
                    } finally {
                        // To avoid hitting rate limits, pause briefly between requests
                        Thread.sleep(500); // 0.5 second delay
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error adding AI recommendations to portfolio data", e);
        }
    }

    /**
     * Prepare technical parameters from a portfolio row for AI analysis.
     */
    private Map<String, Object> prepareTechnicalParametersFromRow(Map<String, Object> row) {
        Map<String, Object> params = new HashMap<>();
        
        // Basic asset information
        params.put("symbol", row.get("symbol"));
        params.put("name", row.get("name"));
        params.put("sector", row.get("sector"));
        
        // Financial metrics
        params.put("profitLossPercentage", row.get("profitLossPercentage"));
        params.put("distanceTo3MonthTarget", row.get("distanceTo3MonthTarget"));
        params.put("currentPrice", row.get("currentPrice"));
        params.put("averagePrice", row.get("averagePrice"));
        
        // Technical indicators
        params.put("rsi", row.get("rsi"));
        params.put("macd", row.get("macd"));
        params.put("sma20", row.get("sma20"));
        params.put("sma50", row.get("sma50"));
        params.put("sma200", row.get("sma200"));
        params.put("priceChange24h", row.get("priceChange24h"));
        params.put("volume24h", row.get("volume24h"));
        params.put("marketCap", row.get("marketCap"));
        
        // Portfolio context
        params.put("holdings", row.get("holdings"));
        params.put("totalAvgCost", row.get("initialValue")); // Use initialValue as totalAvgCost
        params.put("expectedEntry", row.get("expectedEntry"));
        params.put("targetPrice3Month", row.get("targetPrice3Month"));
        params.put("targetPriceLongTerm", row.get("targetPriceLongTerm"));
        
        return params;
    }
    
    /**
     * Generate AI recommendation with explanations for given technical parameters
     */
    private Map<String, Object> getAIRecommendationWithExplanations(Map<String, Object> technicalParams) {
        try {
            // Build AI prompt
            String prompt = buildAIRecommendationPrompt(technicalParams);
            
            // Get AI response
            String aiResponse = callGeminiAPI(prompt);
            
            // Parse AI response
            return parseAIRecommendationResponse(aiResponse);
            
        } catch (Exception e) {
            logger.error("Error getting AI recommendation: " + e.getMessage(), e);
            
            // Return fallback recommendation
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("recommendation", "NEUTRAL");
            fallback.put("confidence", 50);
            fallback.put("explanations", java.util.Arrays.asList("AI analysis temporarily unavailable", "Using default neutral recommendation", "Please check system logs for details"));
            return fallback;
        }
    }
    
    /**
     * Build AI prompt for recommendation generation
     */
    private String buildAIRecommendationPrompt(Map<String, Object> technicalParams) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("As a crypto investment advisor, analyze the following technical parameters and provide a recommendation:\n\n");
        
        // Add technical parameters to prompt
        prompt.append("Symbol: ").append(technicalParams.get("symbol")).append("\n");
        prompt.append("Current Price: $").append(technicalParams.get("currentPrice")).append("\n");
        prompt.append("Holdings: ").append(technicalParams.get("holdings")).append("\n");
        // prompt.append("Expected Entry: $").append(technicalParams.get("expectedEntry")).append("\n");
        // prompt.append("Target Price (3 months): $").append(technicalParams.get("targetPrice3Month")).append("\n");
        // prompt.append("Target Price (Long term): $").append(technicalParams.get("targetPriceLongTerm")).append("\n");
        prompt.append("Support Level: $").append(technicalParams.get("supportLevel")).append("\n");
        prompt.append("Resistance Level: $").append(technicalParams.get("resistanceLevel")).append("\n");
        prompt.append("Stop Loss: $").append(technicalParams.get("stopLoss")).append("\n");
        prompt.append("Take Profit: $").append(technicalParams.get("takeProfit")).append("\n");
        prompt.append("Risk Level: ").append(technicalParams.get("riskLevel")).append("\n");
        prompt.append("Profit/Loss: $").append(technicalParams.get("profitLoss")).append("\n");
        
        prompt.append("\nPlease provide:\n");
        prompt.append("1. A recommendation (STRONG_BUY, BUY, NEUTRAL, SELL, STRONG_SELL)\n");
        prompt.append("2. A confidence score (0-100)\n");
        prompt.append("3. Exactly 3 brief explanations (each max 50 words)\n\n");
        prompt.append("Format your response as JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"recommendation\": \"STRONG_BUY\",\n");
        prompt.append("  \"confidence\": 85,\n");
        prompt.append("  \"explanations\": [\"Explanation 1\", \"Explanation 2\", \"Explanation 3\"]\n");
        prompt.append("}");
        
        return prompt.toString();
    }
    
    /**
     * Parse AI recommendation response from JSON
     */
    private Map<String, Object> parseAIRecommendationResponse(String aiResponse) {
        try {
            // Clean up the response (remove markdown formatting if present)
            String cleanResponse = aiResponse.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();
            
            // Parse JSON response
            JsonNode responseNode = objectMapper.readTree(cleanResponse);
            
            Map<String, Object> result = new HashMap<>();
            result.put("recommendation", responseNode.get("recommendation").asText());
            result.put("confidence", responseNode.get("confidence").asInt());
            
            // Parse explanations array
            JsonNode explanationsNode = responseNode.get("explanations");
            List<String> explanations = new ArrayList<>();
            for (int i = 0; i < Math.min(3, explanationsNode.size()); i++) {
                explanations.add(explanationsNode.get(i).asText());
            }
            result.put("explanations", explanations);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing AI response: " + e.getMessage(), e);
            
            // Fallback parsing - try to extract recommendation from text
            Map<String, Object> fallback = new HashMap<>();
            String upperResponse = aiResponse.toUpperCase();
            
            if (upperResponse.contains("STRONG_BUY")) {
                fallback.put("recommendation", "STRONG_BUY");
                fallback.put("confidence", 80);
            } else if (upperResponse.contains("STRONG_SELL")) {
                fallback.put("recommendation", "STRONG_SELL");
                fallback.put("confidence", 80);
            } else if (upperResponse.contains("BUY")) {
                fallback.put("recommendation", "BUY");
                fallback.put("confidence", 70);
            } else if (upperResponse.contains("SELL")) {
                fallback.put("recommendation", "SELL");
                fallback.put("confidence", 70);
            } else {
                fallback.put("recommendation", "NEUTRAL");
                fallback.put("confidence", 50);
            }
            
            fallback.put("explanations", java.util.Arrays.asList("AI response parsing failed", "Using fallback analysis", "Check system logs for details"));
            return fallback;
        }
    }

    /**
     * Convert numeric confidence score to confidence level string
     */
    private String convertConfidenceToLevel(int confidence) {
        if (confidence >= 80) {
            return "HIGH";
        } else if (confidence >= 60) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
