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
        String prompt = buildPortfolioHealthCheckPrompt(holdings);
        System.out.println("Portfolio Health Check Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Portfolio Health Check AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parsePortfolioHealthCheckResponse(aiResponse);
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
        String prompt = buildPortfolioOptimizationPrompt(holdings);
        System.out.println("Portfolio Optimization Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Portfolio Optimization Analysis AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parsePortfolioOptimizationResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generateEntryExitStrategy(List<Holding> holdings) {
        String prompt = buildEntryExitStrategyPrompt(holdings);
        System.out.println("Entry & Exit Strategy Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Entry & Exit Strategy Analysis AI Response: " + aiResponse);

        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parseEntryExitStrategyResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generateUSDTAllocationStrategy(List<Holding> holdings) {
        double usdtAmount = holdings.stream()
                .filter(h -> h.getSymbol().equalsIgnoreCase("USDT"))
                .findFirst()
                .map(Holding::getHoldings)
                .orElse(0.0);

        String prompt = buildUSDTAllocationPrompt(holdings, usdtAmount);
        System.out.println("USDT Allocation Strategy Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("USDT Allocation Strategy AI Response: " + aiResponse);

        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parseUSDTAllocationResponse(aiResponse);
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

    private String buildPortfolioHealthCheckPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Portfolio Health Check Prompt\n");
        prompt.append("Check the health of my crypto portfolio. Based on the data, tell me:\n");
        prompt.append("- Which coins are over-weighted or under-weighted.\n");
        prompt.append("- Which target prices look unrealistic.\n");
        prompt.append("- Where I should consider taking profit.\n");
        prompt.append("- How much stablecoin buffer I should keep.\n\n");
        
        prompt.append("Portfolio data:\n\n");
        
        // Calculate total portfolio value and individual percentages
        double totalPortfolioValue = 0;
        for (Holding holding : holdings) {
            totalPortfolioValue += holding.getInitialValue();
        }
        
        // Add detailed portfolio information with weightings
        for (Holding holding : holdings) {
            double currentWeight = (holding.getInitialValue() / totalPortfolioValue) * 100;
            
            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Initial Investment: $%.2f (%.1f%% of portfolio)\n", holding.getInitialValue(), currentWeight));
            prompt.append(String.format("Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("Expected Target Price: $%.2f\n", holding.getDeepEntryPrice()));
            prompt.append(String.format("3-Month Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("Long-term Target: $%.2f\n", holding.getTargetPriceLongTerm()));
            
            // Calculate potential returns
            double targetReturn3M = ((holding.getTargetPrice3Month() - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
            double targetReturnLong = ((holding.getTargetPriceLongTerm() - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
            prompt.append(String.format("Expected 3M Return: %.1f%%\n", targetReturn3M));
            prompt.append(String.format("Expected Long Return: %.1f%%\n", targetReturnLong));
            prompt.append("\n");
        }
        
        // Add context and analysis framework
        prompt.append("--- Analysis Framework ---\n");
        prompt.append("For weight analysis:\n");
        prompt.append("- Consider market cap, risk level, and correlation with other holdings\n");
        prompt.append("- Identify concentrations > 15% in single assets or > 30% in correlated sectors\n");
        prompt.append("- Suggest optimal weightings based on risk-adjusted returns\n\n");
        
        prompt.append("For target price analysis:\n");
        prompt.append("- Assess if targets are achievable within timeframes\n");
        prompt.append("- Compare with historical performance and market cycles\n");
        prompt.append("- Identify overly optimistic or conservative projections\n\n");
        
        prompt.append("For profit-taking strategy:\n");
        prompt.append("- Identify positions with >50% gains that should be partially trimmed\n");
        prompt.append("- Suggest ladder selling at key resistance levels\n");
        prompt.append("- Balance between securing profits and maintaining upside exposure\n\n");
        
        prompt.append("For stablecoin buffer:\n");
        prompt.append("- Recommend 5-20% allocation based on market volatility\n");
        prompt.append("- Consider upcoming deployment opportunities\n");
        prompt.append("- Account for emergency liquidity needs\n\n");
        
        prompt.append("--- Context ---\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());
        prompt.append("Portfolio size: $" + String.format("%.0f", totalPortfolioValue) + "\n\n");
        
        prompt.append("--- Output Format ---\n");
        prompt.append("Please provide your health check analysis in the following structured JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"overall_health_score\": \"EXCELLENT|GOOD|FAIR|POOR\",\n");
        prompt.append("  \"health_summary\": \"Brief assessment of portfolio health and key issues\",\n");
        prompt.append("  \"weight_analysis\": {\n");
        prompt.append("    \"overweighted_coins\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"BTC\",\n");
        prompt.append("        \"current_weight\": \"25%\",\n");
        prompt.append("        \"recommended_weight\": \"20-30%\",\n");
        prompt.append("        \"action\": \"Trim position by selling 20% on next rally\",\n");
        prompt.append("        \"reasoning\": \"Concentration risk despite being a safe haven asset\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"underweighted_coins\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"ETH\",\n");
        prompt.append("        \"current_weight\": \"5%\",\n");
        prompt.append("        \"recommended_weight\": \"10-15%\",\n");
        prompt.append("        \"action\": \"Increase allocation during market dips\",\n");
        prompt.append("        \"reasoning\": \"Core infrastructure play with strong fundamentals\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"optimal_weights\": \"Summary of recommended portfolio allocation\"\n");
        prompt.append("  },\n");
        prompt.append("  \"target_price_analysis\": {\n");
        prompt.append("    \"unrealistic_targets\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"SOL\",\n");
        prompt.append("        \"current_target_3m\": \"$300\",\n");
        prompt.append("        \"realistic_target_3m\": \"$180-220\",\n");
        prompt.append("        \"current_target_long\": \"$500\",\n");
        prompt.append("        \"realistic_target_long\": \"$350-400\",\n");
        prompt.append("        \"reasoning\": \"Targets assume continued parabolic growth which is unsustainable\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"conservative_targets\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"AVAX\",\n");
        prompt.append("        \"current_target_long\": \"$100\",\n");
        prompt.append("        \"suggested_target_long\": \"$120-150\",\n");
        prompt.append("        \"reasoning\": \"Strong ecosystem growth potential being underestimated\"\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"profit_taking_strategy\": {\n");
        prompt.append("    \"immediate_candidates\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"SOL\",\n");
        prompt.append("        \"current_gain\": \"120%\",\n");
        prompt.append("        \"action\": \"Sell 25% at $200, 25% at $250\",\n");
        prompt.append("        \"reasoning\": \"Lock in profits while maintaining upside exposure\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"future_candidates\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"AVAX\",\n");
        prompt.append("        \"trigger_gain\": \"50%\",\n");
        prompt.append("        \"action\": \"Begin laddered selling at 50% gains\",\n");
        prompt.append("        \"reasoning\": \"Strong fundamentals justify holding until meaningful gains\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"overall_strategy\": \"Description of profit-taking philosophy and timing\"\n");
        prompt.append("  },\n");
        prompt.append("  \"stablecoin_recommendation\": {\n");
        prompt.append("    \"recommended_percentage\": \"10-15%\",\n");
        prompt.append("    \"reasoning\": \"Rationale for the stablecoin allocation percentage\",\n");
        prompt.append("    \"deployment_strategy\": \"How and when to deploy the stablecoin buffer\",\n");
        prompt.append("    \"suggested_stablecoins\": [\"USDC\", \"USDT\", \"DAI\"],\n");
        prompt.append("    \"yield_opportunities\": \"Staking or lending options for the buffer\"\n");
        prompt.append("  },\n");
        prompt.append("  \"action_priorities\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"priority\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("      \"action\": \"Specific action to take\",\n");
        prompt.append("      \"timeline\": \"When to execute this action\",\n");
        prompt.append("      \"impact\": \"Expected improvement to portfolio health\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"risk_warnings\": [\"List of key risks to monitor\"]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private String callGeminiAPI(String prompt) {
        try {
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

            String response = restTemplate.exchange(
                appConfig.getLlmProvider(),
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
                        return parts.get(0).get("text").asText();
                    }
                }
            }

            return "{}"; // Return empty JSON if parsing fails

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "{}";
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

    private Map<String, Object> parsePortfolioHealthCheckResponse(String response) {
        Map<String, Object> parsedData = new HashMap<>();

        try {
            // Remove markdown code blocks if present
            String cleanResponse = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

            // Extract JSON from response (in case there's extra text)
            String jsonResponse = cleanResponse;
            int jsonStart = cleanResponse.indexOf("{");
            int jsonEnd = cleanResponse.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse = cleanResponse.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode responseNode = objectMapper.readTree(jsonResponse);

            // Parse top-level fields
            parsedData.put("overall_health_score", responseNode.has("overall_health_score") ? responseNode.get("overall_health_score").asText() : "FAIR");
            parsedData.put("health_summary", responseNode.has("health_summary") ? responseNode.get("health_summary").asText() : "Portfolio health check completed");

            // Parse weight analysis
            if (responseNode.has("weight_analysis")) {
                JsonNode weightNode = responseNode.get("weight_analysis");
                Map<String, Object> weightAnalysis = new HashMap<>();

                // Parse overweighted coins
                if (weightNode.has("overweighted_coins")) {
                    List<Map<String, String>> overweighted = new ArrayList<>();
                    JsonNode overweightedArray = weightNode.get("overweighted_coins");

                    for (JsonNode coin : overweightedArray) {
                        Map<String, String> coinData = new HashMap<>();
                        coinData.put("symbol", coin.has("symbol") ? coin.get("symbol").asText() : "");
                        coinData.put("current_weight", coin.has("current_weight") ? coin.get("current_weight").asText() : "");
                        coinData.put("recommended_weight", coin.has("recommended_weight") ? coin.get("recommended_weight").asText() : "");
                        coinData.put("action", coin.has("action") ? coin.get("action").asText() : "");
                        coinData.put("reasoning", coin.has("reasoning") ? coin.get("reasoning").asText() : "");
                        overweighted.add(coinData);
                    }
                    weightAnalysis.put("overweighted_coins", overweighted);
                }

                // Parse underweighted coins
                if (weightNode.has("underweighted_coins")) {
                    List<Map<String, String>> underweighted = new ArrayList<>();
                    JsonNode underweightedArray = weightNode.get("underweighted_coins");

                    for (JsonNode coin : underweightedArray) {
                        Map<String, String> coinData = new HashMap<>();
                        coinData.put("symbol", coin.has("symbol") ? coin.get("symbol").asText() : "");
                        coinData.put("current_weight", coin.has("current_weight") ? coin.get("current_weight").asText() : "");
                        coinData.put("recommended_weight", coin.has("recommended_weight") ? coin.get("recommended_weight").asText() : "");
                        coinData.put("action", coin.has("action") ? coin.get("action").asText() : "");
                        coinData.put("reasoning", coin.has("reasoning") ? coin.get("reasoning").asText() : "");
                        underweighted.add(coinData);
                    }
                    weightAnalysis.put("underweighted_coins", underweighted);
                }

                weightAnalysis.put("optimal_weights", weightNode.has("optimal_weights") ? weightNode.get("optimal_weights").asText() : "");
                parsedData.put("weight_analysis", weightAnalysis);
            }

            // Parse target price analysis
            if (responseNode.has("target_price_analysis")) {
                JsonNode targetNode = responseNode.get("target_price_analysis");
                Map<String, Object> targetAnalysis = new HashMap<>();

                // Parse unrealistic targets
                if (targetNode.has("unrealistic_targets")) {
                    List<Map<String, String>> unrealistic = new ArrayList<>();
                    JsonNode unrealisticArray = targetNode.get("unrealistic_targets");

                    for (JsonNode target : unrealisticArray) {
                        Map<String, String> targetData = new HashMap<>();
                        targetData.put("symbol", target.has("symbol") ? target.get("symbol").asText() : "");
                        targetData.put("current_target_3m", target.has("current_target_3m") ? target.get("current_target_3m").asText() : "");
                        targetData.put("realistic_target_3m", target.has("realistic_target_3m") ? target.get("realistic_target_3m").asText() : "");
                        targetData.put("current_target_long", target.has("current_target_long") ? target.get("current_target_long").asText() : "");
                        targetData.put("realistic_target_long", target.has("realistic_target_long") ? target.get("realistic_target_long").asText() : "");
                        targetData.put("reasoning", target.has("reasoning") ? target.get("reasoning").asText() : "");
                        unrealistic.add(targetData);
                    }
                    targetAnalysis.put("unrealistic_targets", unrealistic);
                }

                // Parse conservative targets
                if (targetNode.has("conservative_targets")) {
                    List<Map<String, String>> conservative = new ArrayList<>();
                    JsonNode conservativeArray = targetNode.get("conservative_targets");

                    for (JsonNode target : conservativeArray) {
                        Map<String, String> targetData = new HashMap<>();
                        targetData.put("symbol", target.has("symbol") ? target.get("symbol").asText() : "");
                        targetData.put("current_target_long", target.has("current_target_long") ? target.get("current_target_long").asText() : "");
                        targetData.put("suggested_target_long", target.has("suggested_target_long") ? target.get("suggested_target_long").asText() : "");
                        targetData.put("reasoning", target.has("reasoning") ? target.get("reasoning").asText() : "");
                        conservative.add(targetData);
                    }
                    targetAnalysis.put("conservative_targets", conservative);
                }

                parsedData.put("target_price_analysis", targetAnalysis);
            }

            // Parse profit taking strategy
            if (responseNode.has("profit_taking_strategy")) {
                JsonNode profitNode = responseNode.get("profit_taking_strategy");
                Map<String, Object> profitStrategy = new HashMap<>();

                // Parse immediate candidates
                if (profitNode.has("immediate_candidates")) {
                    List<Map<String, String>> immediate = new ArrayList<>();
                    JsonNode immediateArray = profitNode.get("immediate_candidates");

                    for (JsonNode candidate : immediateArray) {
                        Map<String, String> candidateData = new HashMap<>();
                        candidateData.put("symbol", candidate.has("symbol") ? candidate.get("symbol").asText() : "");
                        candidateData.put("current_gain", candidate.has("current_gain") ? candidate.get("current_gain").asText() : "");
                        candidateData.put("action", candidate.has("action") ? candidate.get("action").asText() : "");
                        candidateData.put("reasoning", candidate.has("reasoning") ? candidate.get("reasoning").asText() : "");
                        immediate.add(candidateData);
                    }
                    profitStrategy.put("immediate_candidates", immediate);
                }

                // Parse future candidates
                if (profitNode.has("future_candidates")) {
                    List<Map<String, String>> future = new ArrayList<>();
                    JsonNode futureArray = profitNode.get("future_candidates");

                    for (JsonNode candidate : futureArray) {
                        Map<String, String> candidateData = new HashMap<>();
                        candidateData.put("symbol", candidate.has("symbol") ? candidate.get("symbol").asText() : "");
                        candidateData.put("trigger_gain", candidate.has("trigger_gain") ? candidate.get("trigger_gain").asText() : "");
                        candidateData.put("action", candidate.has("action") ? candidate.get("action").asText() : "");
                        candidateData.put("reasoning", candidate.has("reasoning") ? candidate.get("reasoning").asText() : "");
                        future.add(candidateData);
                    }
                    profitStrategy.put("future_candidates", future);
                }

                profitStrategy.put("overall_strategy", profitNode.has("overall_strategy") ? profitNode.get("overall_strategy").asText() : "");
                parsedData.put("profit_taking_strategy", profitStrategy);
            }

            // Parse stablecoin recommendation
            if (responseNode.has("stablecoin_recommendation")) {
                JsonNode stablecoinNode = responseNode.get("stablecoin_recommendation");
                Map<String, Object> stablecoinRec = new HashMap<>();

                stablecoinRec.put("recommended_percentage", stablecoinNode.has("recommended_percentage") ? stablecoinNode.get("recommended_percentage").asText() : "10%");
                stablecoinRec.put("reasoning", stablecoinNode.has("reasoning") ? stablecoinNode.get("reasoning").asText() : "");
                stablecoinRec.put("deployment_strategy", stablecoinNode.has("deployment_strategy") ? stablecoinNode.get("deployment_strategy").asText() : "");
                stablecoinRec.put("yield_opportunities", stablecoinNode.has("yield_opportunities") ? stablecoinNode.get("yield_opportunities").asText() : "");

                // Parse suggested stablecoins
                if (stablecoinNode.has("suggested_stablecoins")) {
                    List<String> suggestedStablecoins = new ArrayList<>();
                    JsonNode stablecoinsArray = stablecoinNode.get("suggested_stablecoins");
                    for (JsonNode stablecoin : stablecoinsArray) {
                        suggestedStablecoins.add(stablecoin.asText());
                    }
                    stablecoinRec.put("suggested_stablecoins", suggestedStablecoins);
                }

                parsedData.put("stablecoin_recommendation", stablecoinRec);
            }

            // Parse action priorities
            if (responseNode.has("action_priorities")) {
                List<Map<String, String>> priorities = new ArrayList<>();
                JsonNode prioritiesArray = responseNode.get("action_priorities");

                for (JsonNode priority : prioritiesArray) {
                    Map<String, String> priorityData = new HashMap<>();
                    priorityData.put("priority", priority.has("priority") ? priority.get("priority").asText() : "MEDIUM");
                    priorityData.put("action", priority.has("action") ? priority.get("action").asText() : "");
                    priorityData.put("timeline", priority.has("timeline") ? priority.get("timeline").asText() : "");
                    priorityData.put("impact", priority.has("impact") ? priority.get("impact").asText() : "");
                    priorities.add(priorityData);
                }
                parsedData.put("action_priorities", priorities);
            }

            // Parse risk warnings
            if (responseNode.has("risk_warnings")) {
                List<String> riskWarnings = new ArrayList<>();
                JsonNode warningsArray = responseNode.get("risk_warnings");
                for (JsonNode warning : warningsArray) {
                    riskWarnings.add(warning.asText());
                }
                parsedData.put("risk_warnings", riskWarnings);
            }

        } catch (Exception e) {
            System.err.println("Error parsing portfolio health check response: " + e.getMessage());
            e.printStackTrace();

            // Set default values if parsing fails
            parsedData.put("overall_health_score", "FAIR");
            parsedData.put("health_summary", "Unable to parse health check due to API response parsing error");
            parsedData.put("weight_analysis", new HashMap<>());
            parsedData.put("target_price_analysis", new HashMap<>());
            parsedData.put("profit_taking_strategy", new HashMap<>());
            parsedData.put("stablecoin_recommendation", new HashMap<>());
            parsedData.put("action_priorities", new ArrayList<>());
            parsedData.put("risk_warnings", new ArrayList<>());
        }

        return parsedData;
    }

    private String buildPortfolioOptimizationPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("🔍 Portfolio Optimization Analysis Prompt\n");
        prompt.append("**Please provide insights on the following:**\n\n");
        
        prompt.append("1. **Overall risk profile** of the portfolio — consider asset class exposure (Layer 1s, DeFi, etc.), volatility, and diversification.\n");
        prompt.append("2. How well is the portfolio **balanced between upside potential and downside risk**?\n");
        prompt.append("3. Which assets appear **overweighted or underweighted** based on their fundamentals, growth outlook, and risk levels?\n");
        prompt.append("4. **Actionable suggestions to rebalance** the portfolio for stronger long-term opportunity capture (e.g., adding exposure to AI, DeFi, Web3 infrastructure).\n");
        prompt.append("5. Are there any **key sectors or assets missing** that I should consider for better diversification? (e.g., stablecoins, staking coins, interoperability tokens, or blue-chip infrastructure projects).\n\n");
        
        prompt.append("⚠️ **Context & Preferences:**\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());
        prompt.append(investmentStrategyService.getPortfolioTargetsSection());
        prompt.append(investmentStrategyService.getRiskManagementSection());
        
        prompt.append("I'm seeking a thoughtful, data-driven analysis to optimize my portfolio for both growth and risk management.\n\n");
        
        prompt.append("Portfolio data:\n\n");
        
        // Calculate total portfolio value and individual percentages
        double totalPortfolioValue = 0;
        for (Holding holding : holdings) {
            totalPortfolioValue += holding.getInitialValue();
        }
        
        // Add detailed portfolio information with current weightings
        for (Holding holding : holdings) {
            double currentWeight = (holding.getInitialValue() / totalPortfolioValue) * 100;
            
            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Initial Investment: $%.2f (%.1f%% of portfolio)\n", holding.getInitialValue(), currentWeight));
            prompt.append(String.format("Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("Expected Deep Entry Price: $%.2f\n", holding.getDeepEntryPrice()));
            prompt.append(String.format("3-Month Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("Long-term Target: $%.2f\n", holding.getTargetPriceLongTerm()));
            
            // Calculate potential returns and risk metrics
            double targetReturn3M = ((holding.getTargetPrice3Month() - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
            double targetReturnLong = ((holding.getTargetPriceLongTerm() - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
            double riskRewardRatio3M = targetReturn3M / Math.max(currentWeight, 1.0); // Simple risk-adjusted return proxy
            
            prompt.append(String.format("Expected 3M Return: %.1f%%\n", targetReturn3M));
            prompt.append(String.format("Expected Long Return: %.1f%%\n", targetReturnLong));
            prompt.append(String.format("Risk-Adjusted Return (3M): %.2f\n", riskRewardRatio3M));
            prompt.append("\n");
        }
        
        // Add portfolio summary context
        prompt.append("--- Portfolio Summary ---\n");
        prompt.append(String.format("Total Portfolio Value: $%.0f\n", totalPortfolioValue));
        prompt.append(String.format("Number of Assets: %d\n", holdings.size()));
        prompt.append("Investment focus: Moderate risk tolerance with growth orientation\n");
        prompt.append("Time horizon: Medium to long-term (6 months to 3 years)\n\n");
        
        prompt.append("--- Analysis Framework ---\n");
        prompt.append("For risk profile assessment:\n");
        prompt.append("- Analyze sector concentration (Layer 1s, Layer 2s, DeFi, AI/ML, Infrastructure, etc.)\n");
        prompt.append("- Evaluate correlation risks between holdings\n");
        prompt.append("- Assess volatility exposure and potential drawdown scenarios\n");
        prompt.append("- Consider regulatory and technological risks\n\n");
        
        prompt.append("For upside/downside balance:\n");
        prompt.append("- Compare growth potential vs. stability characteristics\n");
        prompt.append("- Evaluate defensive assets vs. high-growth opportunities\n");
        prompt.append("- Consider portfolio's resilience in bear market scenarios\n");
        prompt.append("- Assess asymmetric risk-reward opportunities\n\n");
        
        prompt.append("For weight optimization:\n");
        prompt.append("- Compare current allocations vs. market cap weightings\n");
        prompt.append("- Evaluate fundamental strength vs. position size\n");
        prompt.append("- Consider growth outlook vs. current valuation levels\n");
        prompt.append("- Identify concentration risks and diversification gaps\n\n");
        
        prompt.append("For rebalancing recommendations:\n");
        prompt.append("- Suggest specific percentage adjustments for existing holdings\n");
        prompt.append("- Recommend entry points for new positions\n");
        prompt.append("- Prioritize actions based on impact and urgency\n");
        prompt.append("- Consider tax implications and transaction costs\n\n");
        
        prompt.append("For missing assets/sectors:\n");
        prompt.append("- Identify key blockchain ecosystems or use cases not represented\n");
        prompt.append("- Suggest specific blue-chip projects with strong fundamentals\n");
        prompt.append("- Recommend stablecoin allocation for stability and yield\n");
        prompt.append("- Consider infrastructure plays (oracles, bridges, security)\n\n");
        
        prompt.append("--- Output Format ---\n");
        prompt.append("Please provide your optimization analysis in the following structured JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"optimization_score\": \"EXCELLENT|GOOD|FAIR|NEEDS_IMPROVEMENT|POOR\",\n");
        prompt.append("  \"executive_summary\": \"Brief assessment of portfolio optimization needs and key recommendations\",\n");
        prompt.append("  \"risk_profile_analysis\": {\n");
        prompt.append("    \"overall_risk_level\": \"CONSERVATIVE|MODERATE|AGGRESSIVE|VERY_AGGRESSIVE\",\n");
        prompt.append("    \"sector_concentration\": {\n");
        prompt.append("      \"layer1_percentage\": \"45%\",\n");
        prompt.append("      \"layer2_percentage\": \"15%\",\n");
        prompt.append("      \"defi_percentage\": \"20%\",\n");
        prompt.append("      \"ai_ml_percentage\": \"10%\",\n");
        prompt.append("      \"infrastructure_percentage\": \"5%\",\n");
        prompt.append("      \"other_percentage\": \"5%\"\n");
        prompt.append("    },\n");
        prompt.append("    \"correlation_risks\": \"Assessment of portfolio correlation and concentration risks\",\n");
        prompt.append("    \"volatility_assessment\": \"Expected portfolio volatility and drawdown potential\",\n");
        prompt.append("    \"main_risk_factors\": [\"List of primary risk factors to monitor\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"upside_downside_balance\": {\n");
        prompt.append("    \"balance_score\": \"EXCELLENT|GOOD|FAIR|POOR\",\n");
        prompt.append("    \"growth_potential\": \"Assessment of portfolio's growth potential\",\n");
        prompt.append("    \"downside_protection\": \"Evaluation of defensive characteristics\",\n");
        prompt.append("    \"asymmetric_opportunities\": \"Identification of high-reward, limited-risk opportunities\",\n");
        prompt.append("    \"bear_market_resilience\": \"How well the portfolio might perform in market downturns\"\n");
        prompt.append("  },\n");
        prompt.append("  \"weight_optimization\": {\n");
        prompt.append("    \"overweighted_assets\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"BTC\",\n");
        prompt.append("        \"current_weight\": \"25%\",\n");
        prompt.append("        \"optimal_weight\": \"20-22%\",\n");
        prompt.append("        \"reduction_amount\": \"3-5%\",\n");
        prompt.append("        \"rationale\": \"Slightly overweight relative to risk-adjusted return potential\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"underweighted_assets\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"ETH\",\n");
        prompt.append("        \"current_weight\": \"8%\",\n");
        prompt.append("        \"optimal_weight\": \"15-18%\",\n");
        prompt.append("        \"increase_amount\": \"7-10%\",\n");
        prompt.append("        \"rationale\": \"Strong fundamentals and ecosystem growth justify larger allocation\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"well_positioned_assets\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"SOL\",\n");
        prompt.append("        \"current_weight\": \"12%\",\n");
        prompt.append("        \"rationale\": \"Appropriate allocation given risk-reward profile\"\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"rebalancing_strategy\": {\n");
        prompt.append("    \"immediate_actions\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"action\": \"Trim BTC position by 3%\",\n");
        prompt.append("        \"priority\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("        \"execution_strategy\": \"Sell on strength above $95,000\",\n");
        prompt.append("        \"expected_impact\": \"Reduce concentration risk while maintaining core exposure\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"medium_term_actions\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"action\": \"Increase ETH allocation during market dips\",\n");
        prompt.append("        \"target_timing\": \"Next 3-6 months during any 15%+ correction\",\n");
        prompt.append("        \"rationale\": \"Strong ecosystem fundamentals at attractive entry points\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"optimal_allocation_targets\": \"Summary of ideal portfolio allocation percentages\"\n");
        prompt.append("  },\n");
        prompt.append("  \"missing_assets_analysis\": {\n");
        prompt.append("    \"critical_gaps\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"sector\": \"Stablecoins/Yield\",\n");
        prompt.append("        \"current_allocation\": \"0%\",\n");
        prompt.append("        \"recommended_allocation\": \"8-12%\",\n");
        prompt.append("        \"suggested_assets\": [\"USDC\", \"DAI\", \"stETH\"],\n");
        prompt.append("        \"benefits\": \"Stability, yield generation, and dry powder for opportunities\",\n");
        prompt.append("        \"implementation_priority\": \"HIGH\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"strategic_additions\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"sector\": \"AI/Machine Learning\",\n");
        prompt.append("        \"current_allocation\": \"2%\",\n");
        prompt.append("        \"recommended_allocation\": \"8-12%\",\n");
        prompt.append("        \"suggested_assets\": [\"FET\", \"RNDR\", \"TAO\"],\n");
        prompt.append("        \"rationale\": \"High-growth sector with strong fundamental drivers\",\n");
        prompt.append("        \"implementation_priority\": \"MEDIUM\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"blue_chip_opportunities\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"asset\": \"LINK\",\n");
        prompt.append("        \"sector\": \"Infrastructure/Oracles\",\n");
        prompt.append("        \"allocation_suggestion\": \"3-5%\",\n");
        prompt.append("        \"fundamental_strength\": \"Market-leading oracle network with strong adoption\",\n");
        prompt.append("        \"risk_level\": \"MODERATE\"\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"implementation_roadmap\": {\n");
        prompt.append("    \"phase_1_immediate\": [\n");
        prompt.append("      \"Actions to take within 1 month\"\n");
        prompt.append("    ],\n");
        prompt.append("    \"phase_2_short_term\": [\n");
        prompt.append("      \"Actions for 1-6 months\"\n");
        prompt.append("    ],\n");
        prompt.append("    \"phase_3_long_term\": [\n");
        prompt.append("      \"Strategic moves for 6 months to 2 years\"\n");
        prompt.append("    ],\n");
        prompt.append("    \"success_metrics\": [\"Key indicators to track optimization progress\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"risk_management_recommendations\": [\n");
        prompt.append("    \"Specific risk mitigation strategies and position sizing guidelines\"\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private Map<String, Object> parsePortfolioOptimizationResponse(String response) {
        Map<String, Object> parsedData = new HashMap<>();
        
        try {
            // Remove markdown code blocks if present
            String cleanResponse = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            
            // Extract JSON from response (in case there's extra text)
            String jsonResponse = cleanResponse;
            int jsonStart = cleanResponse.indexOf("{");
            int jsonEnd = cleanResponse.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse = cleanResponse.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode responseNode = objectMapper.readTree(jsonResponse);
            
            // Parse top-level fields
            parsedData.put("optimization_score", responseNode.has("optimization_score") ? responseNode.get("optimization_score").asText() : "FAIR");
            parsedData.put("executive_summary", responseNode.has("executive_summary") ? responseNode.get("executive_summary").asText() : "Portfolio optimization analysis completed");
            
            // Parse risk profile analysis
            if (responseNode.has("risk_profile_analysis")) {
                JsonNode riskNode = responseNode.get("risk_profile_analysis");
                Map<String, Object> riskAnalysis = new HashMap<>();
                
                riskAnalysis.put("overall_risk_level", riskNode.has("overall_risk_level") ? riskNode.get("overall_risk_level").asText() : "MODERATE");
                
                // Parse sector concentration
                if (riskNode.has("sector_concentration")) {
                    JsonNode sectorNode = riskNode.get("sector_concentration");
                    Map<String, String> sectorConcentration = new HashMap<>();
                    sectorConcentration.put("layer1_percentage", sectorNode.has("layer1_percentage") ? sectorNode.get("layer1_percentage").asText() : "0%");
                    sectorConcentration.put("layer2_percentage", sectorNode.has("layer2_percentage") ? sectorNode.get("layer2_percentage").asText() : "0%");
                    sectorConcentration.put("defi_percentage", sectorNode.has("defi_percentage") ? sectorNode.get("defi_percentage").asText() : "0%");
                    sectorConcentration.put("ai_ml_percentage", sectorNode.has("ai_ml_percentage") ? sectorNode.get("ai_ml_percentage").asText() : "0%");
                    sectorConcentration.put("infrastructure_percentage", sectorNode.has("infrastructure_percentage") ? sectorNode.get("infrastructure_percentage").asText() : "0%");
                    sectorConcentration.put("other_percentage", sectorNode.has("other_percentage") ? sectorNode.get("other_percentage").asText() : "0%");
                    riskAnalysis.put("sector_concentration", sectorConcentration);
                }
                
                riskAnalysis.put("correlation_risks", riskNode.has("correlation_risks") ? riskNode.get("correlation_risks").asText() : "");
                riskAnalysis.put("volatility_assessment", riskNode.has("volatility_assessment") ? riskNode.get("volatility_assessment").asText() : "");
                
                // Parse main risk factors
                if (riskNode.has("main_risk_factors")) {
                    List<String> riskFactors = new ArrayList<>();
                    JsonNode riskFactorsArray = riskNode.get("main_risk_factors");
                    for (JsonNode factor : riskFactorsArray) {
                        riskFactors.add(factor.asText());
                    }
                    riskAnalysis.put("main_risk_factors", riskFactors);
                }
                
                parsedData.put("risk_profile_analysis", riskAnalysis);
            }
            
            // Parse upside/downside balance
            if (responseNode.has("upside_downside_balance")) {
                JsonNode balanceNode = responseNode.get("upside_downside_balance");
                Map<String, String> balanceAnalysis = new HashMap<>();
                balanceAnalysis.put("balance_score", balanceNode.has("balance_score") ? balanceNode.get("balance_score").asText() : "FAIR");
                balanceAnalysis.put("growth_potential", balanceNode.has("growth_potential") ? balanceNode.get("growth_potential").asText() : "");
                balanceAnalysis.put("downside_protection", balanceNode.has("downside_protection") ? balanceNode.get("downside_protection").asText() : "");
                balanceAnalysis.put("asymmetric_opportunities", balanceNode.has("asymmetric_opportunities") ? balanceNode.get("asymmetric_opportunities").asText() : "");
                balanceAnalysis.put("bear_market_resilience", balanceNode.has("bear_market_resilience") ? balanceNode.get("bear_market_resilience").asText() : "");
                parsedData.put("upside_downside_balance", balanceAnalysis);
            }
            
            // Parse weight optimization
            if (responseNode.has("weight_optimization")) {
                JsonNode weightNode = responseNode.get("weight_optimization");
                Map<String, Object> weightOptimization = new HashMap<>();
                
                // Parse overweighted assets
                if (weightNode.has("overweighted_assets")) {
                    List<Map<String, String>> overweighted = new ArrayList<>();
                    JsonNode overweightedArray = weightNode.get("overweighted_assets");
                    
                    for (JsonNode asset : overweightedArray) {
                        Map<String, String> assetData = new HashMap<>();
                        assetData.put("symbol", asset.has("symbol") ? asset.get("symbol").asText() : "");
                        assetData.put("current_weight", asset.has("current_weight") ? asset.get("current_weight").asText() : "");
                        assetData.put("optimal_weight", asset.has("optimal_weight") ? asset.get("optimal_weight").asText() : "");
                        assetData.put("reduction_amount", asset.has("reduction_amount") ? asset.get("reduction_amount").asText() : "");
                        assetData.put("rationale", asset.has("rationale") ? asset.get("rationale").asText() : "");
                        overweighted.add(assetData);
                    }
                    weightOptimization.put("overweighted_assets", overweighted);
                }
                
                // Parse underweighted assets
                if (weightNode.has("underweighted_assets")) {
                    List<Map<String, String>> underweighted = new ArrayList<>();
                    JsonNode underweightedArray = weightNode.get("underweighted_assets");
                    
                    for (JsonNode asset : underweightedArray) {
                        Map<String, String> assetData = new HashMap<>();
                        assetData.put("symbol", asset.has("symbol") ? asset.get("symbol").asText() : "");
                        assetData.put("current_weight", asset.has("current_weight") ? asset.get("current_weight").asText() : "");
                        assetData.put("optimal_weight", asset.has("optimal_weight") ? asset.get("optimal_weight").asText() : "");
                        assetData.put("increase_amount", asset.has("increase_amount") ? asset.get("increase_amount").asText() : "");
                        assetData.put("rationale", asset.has("rationale") ? asset.get("rationale").asText() : "");
                        underweighted.add(assetData);
                    }
                    weightOptimization.put("underweighted_assets", underweighted);
                }
                
                // Parse well positioned assets
                if (weightNode.has("well_positioned_assets")) {
                    List<Map<String, String>> wellPositioned = new ArrayList<>();
                    JsonNode wellPositionedArray = weightNode.get("well_positioned_assets");
                    
                    for (JsonNode asset : wellPositionedArray) {
                        Map<String, String> assetData = new HashMap<>();
                        assetData.put("symbol", asset.has("symbol") ? asset.get("symbol").asText() : "");
                        assetData.put("current_weight", asset.has("current_weight") ? asset.get("current_weight").asText() : "");
                        assetData.put("rationale", asset.has("rationale") ? asset.get("rationale").asText() : "");
                        wellPositioned.add(assetData);
                    }
                    weightOptimization.put("well_positioned_assets", wellPositioned);
                }
                
                parsedData.put("weight_optimization", weightOptimization);
            }
            
            // Parse rebalancing strategy
            if (responseNode.has("rebalancing_strategy")) {
                JsonNode rebalanceNode = responseNode.get("rebalancing_strategy");
                Map<String, Object> rebalanceStrategy = new HashMap<>();
                
                // Parse immediate actions
                if (rebalanceNode.has("immediate_actions")) {
                    List<Map<String, String>> immediateActions = new ArrayList<>();
                    JsonNode actionsArray = rebalanceNode.get("immediate_actions");
                    
                    for (JsonNode action : actionsArray) {
                        Map<String, String> actionData = new HashMap<>();
                        actionData.put("action", action.has("action") ? action.get("action").asText() : "");
                        actionData.put("priority", action.has("priority") ? action.get("priority").asText() : "MEDIUM");
                        actionData.put("execution_strategy", action.has("execution_strategy") ? action.get("execution_strategy").asText() : "");
                        actionData.put("expected_impact", action.has("expected_impact") ? action.get("expected_impact").asText() : "");
                        immediateActions.add(actionData);
                    }
                    rebalanceStrategy.put("immediate_actions", immediateActions);
                }
                
                // Parse medium term actions
                if (rebalanceNode.has("medium_term_actions")) {
                    List<Map<String, String>> mediumActions = new ArrayList<>();
                    JsonNode actionsArray = rebalanceNode.get("medium_term_actions");
                    
                    for (JsonNode action : actionsArray) {
                        Map<String, String> actionData = new HashMap<>();
                        actionData.put("action", action.has("action") ? action.get("action").asText() : "");
                        actionData.put("target_timing", action.has("target_timing") ? action.get("target_timing").asText() : "");
                        actionData.put("rationale", action.has("rationale") ? action.get("rationale").asText() : "");
                        mediumActions.add(actionData);
                    }
                    rebalanceStrategy.put("medium_term_actions", mediumActions);
                }
                
                rebalanceStrategy.put("optimal_allocation_targets", rebalanceNode.has("optimal_allocation_targets") ? rebalanceNode.get("optimal_allocation_targets").asText() : "");
                parsedData.put("rebalancing_strategy", rebalanceStrategy);
            }
            
            // Parse missing assets analysis
            if (responseNode.has("missing_assets_analysis")) {
                JsonNode missingNode = responseNode.get("missing_assets_analysis");
                Map<String, Object> missingAnalysis = new HashMap<>();
                
                // Parse critical gaps
                if (missingNode.has("critical_gaps")) {
                    List<Map<String, Object>> criticalGaps = new ArrayList<>();
                    JsonNode gapsArray = missingNode.get("critical_gaps");
                    
                    for (JsonNode gap : gapsArray) {
                        Map<String, Object> gapData = new HashMap<>();
                        gapData.put("sector", gap.has("sector") ? gap.get("sector").asText() : "");
                        gapData.put("current_allocation", gap.has("current_allocation") ? gap.get("current_allocation").asText() : "0%");
                        gapData.put("recommended_allocation", gap.has("recommended_allocation") ? gap.get("recommended_allocation").asText() : "5%");
                        gapData.put("benefits", gap.has("benefits") ? gap.get("benefits").asText() : "");
                        gapData.put("implementation_priority", gap.has("implementation_priority") ? gap.get("implementation_priority").asText() : "MEDIUM");
                        
                        // Parse suggested assets
                        if (gap.has("suggested_assets")) {
                            List<String> suggestedAssets = new ArrayList<>();
                            JsonNode assetsArray = gap.get("suggested_assets");
                            for (JsonNode asset : assetsArray) {
                                suggestedAssets.add(asset.asText());
                            }
                            gapData.put("suggested_assets", suggestedAssets);
                        }
                        
                        criticalGaps.add(gapData);
                    }
                    missingAnalysis.put("critical_gaps", criticalGaps);
                }
                
                // Parse strategic additions
                if (missingNode.has("strategic_additions")) {
                    List<Map<String, Object>> strategicAdditions = new ArrayList<>();
                    JsonNode additionsArray = missingNode.get("strategic_additions");
                    
                    for (JsonNode addition : additionsArray) {
                        Map<String, Object> additionData = new HashMap<>();
                        additionData.put("sector", addition.has("sector") ? addition.get("sector").asText() : "");
                        additionData.put("current_allocation", addition.has("current_allocation") ? addition.get("current_allocation").asText() : "0%");
                        additionData.put("recommended_allocation", addition.has("recommended_allocation") ? addition.get("recommended_allocation").asText() : "5%");
                        additionData.put("rationale", addition.has("rationale") ? addition.get("rationale").asText() : "");
                        additionData.put("implementation_priority", addition.has("implementation_priority") ? addition.get("implementation_priority").asText() : "MEDIUM");
                        
                        // Parse suggested assets
                        if (addition.has("suggested_assets")) {
                            List<String> suggestedAssets = new ArrayList<>();
                            JsonNode assetsArray = addition.get("suggested_assets");
                            for (JsonNode asset : assetsArray) {
                                suggestedAssets.add(asset.asText());
                            }
                            additionData.put("suggested_assets", suggestedAssets);
                        }
                        
                        strategicAdditions.add(additionData);
                    }
                    missingAnalysis.put("strategic_additions", strategicAdditions);
                }
                
                // Parse blue chip opportunities
                if (missingNode.has("blue_chip_opportunities")) {
                    List<Map<String, String>> blueChipOpps = new ArrayList<>();
                    JsonNode oppsArray = missingNode.get("blue_chip_opportunities");
                    
                    for (JsonNode opp : oppsArray) {
                        Map<String, String> oppData = new HashMap<>();
                        oppData.put("asset", opp.has("asset") ? opp.get("asset").asText() : "");
                        oppData.put("sector", opp.has("sector") ? opp.get("sector").asText() : "");
                        oppData.put("allocation_suggestion", opp.has("allocation_suggestion") ? opp.get("allocation_suggestion").asText() : "3-5%");
                        oppData.put("fundamental_strength", opp.has("fundamental_strength") ? opp.get("fundamental_strength").asText() : "");
                        oppData.put("risk_level", opp.has("risk_level") ? opp.get("risk_level").asText() : "MODERATE");
                        blueChipOpps.add(oppData);
                    }
                    missingAnalysis.put("blue_chip_opportunities", blueChipOpps);
                }
                
                parsedData.put("missing_assets_analysis", missingAnalysis);
            }
            
            // Parse implementation roadmap
            if (responseNode.has("implementation_roadmap")) {
                JsonNode roadmapNode = responseNode.get("implementation_roadmap");
                Map<String, Object> roadmap = new HashMap<>();
                
                // Parse phases
                if (roadmapNode.has("phase_1_immediate")) {
                    List<String> phase1 = new ArrayList<>();
                    JsonNode phase1Array = roadmapNode.get("phase_1_immediate");
                    for (JsonNode item : phase1Array) {
                        phase1.add(item.asText());
                    }
                    roadmap.put("phase_1_immediate", phase1);
                }
                
                if (roadmapNode.has("phase_2_short_term")) {
                    List<String> phase2 = new ArrayList<>();
                    JsonNode phase2Array = roadmapNode.get("phase_2_short_term");
                    for (JsonNode item : phase2Array) {
                        phase2.add(item.asText());
                    }
                    roadmap.put("phase_2_short_term", phase2);
                }
                
                if (roadmapNode.has("phase_3_long_term")) {
                    List<String> phase3 = new ArrayList<>();
                    JsonNode phase3Array = roadmapNode.get("phase_3_long_term");
                    for (JsonNode item : phase3Array) {
                        phase3.add(item.asText());
                    }
                    roadmap.put("phase_3_long_term", phase3);
                }
                
                if (roadmapNode.has("success_metrics")) {
                    List<String> metrics = new ArrayList<>();
                    JsonNode metricsArray = roadmapNode.get("success_metrics");
                    for (JsonNode metric : metricsArray) {
                        metrics.add(metric.asText());
                    }
                    roadmap.put("success_metrics", metrics);
                }
                
                parsedData.put("implementation_roadmap", roadmap);
            }
            
            // Parse risk management recommendations
            if (responseNode.has("risk_management_recommendations")) {
                List<String> riskMgmt = new ArrayList<>();
                JsonNode riskMgmtArray = responseNode.get("risk_management_recommendations");
                for (JsonNode recommendation : riskMgmtArray) {
                    riskMgmt.add(recommendation.asText());
                }
                parsedData.put("risk_management_recommendations", riskMgmt);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing portfolio optimization response: " + e.getMessage());
            e.printStackTrace();
            
            // Set default values if parsing fails
            parsedData.put("optimization_score", "FAIR");
            parsedData.put("executive_summary", "Unable to parse portfolio optimization analysis due to API response parsing error");
            parsedData.put("risk_profile_analysis", new HashMap<>());
            parsedData.put("upside_downside_balance", new HashMap<>());
            parsedData.put("weight_optimization", new HashMap<>());
            parsedData.put("rebalancing_strategy", new HashMap<>());
            parsedData.put("missing_assets_analysis", new HashMap<>());
            parsedData.put("implementation_roadmap", new HashMap<>());
            parsedData.put("risk_management_recommendations", new ArrayList<>());
        }
        
        return parsedData;
    }

    // Investment Analysis Methods
    public Map<String, Object> generateInvestmentAnalysis(Holding holding) {
        try {
            // Fetch market data for the specified crypto using DataProviderService
            MarketData marketData = dataProviderService.getMarketData(holding.getId());
            
            // Convert MarketData to Map for compatibility with prompt builder
            Map<String, Object> marketDataMap = convertMarketDataToMap(marketData);
            
            // Build prompt for comprehensive investment analysis
            String prompt = buildInvestmentAnalysisPrompt(holding, marketDataMap);
            
            // Get AI analysis using Gemini API
            String aiResponse = callGeminiAPI(prompt);
            
            // Parse and structure the response
            return parseInvestmentAnalysisResponse(aiResponse, holding.getSymbol());
            
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

    private String buildInvestmentAnalysisPrompt(Holding holding, Map<String, Object> marketData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a professional crypto investment analyst providing a comprehensive ").append(holding.getSymbol()).append(" investment analysis. ");
        prompt.append("Analyze the current ").append(holding.getSymbol()).append(" market conditions and provide a detailed, data-backed evaluation following this EXACT structure:\n\n");
        
        prompt.append("Current ").append(holding.getSymbol()).append(" Data:\n");
        prompt.append("- Price: $").append(marketData.get("price")).append("\n");
        prompt.append("- 24h Change: ").append(marketData.get("priceChange24h")).append("%\n");
        prompt.append("- 7d Change: ").append(marketData.get("priceChange7d")).append("%\n");
        prompt.append("- Market Cap: $").append(marketData.get("marketCap")).append("\n");
        prompt.append("- Volume 24h: $").append(marketData.get("volume24h")).append("\n\n");
        
        // Include holding-specific context if available
        if (holding.getAveragePrice() > 0) {
            prompt.append("Current Position Context:\n");
            prompt.append("- Your average buy price: $").append(holding.getAveragePrice()).append("\n");
            prompt.append("- Holdings: ").append(holding.getHoldings()).append(" ").append(holding.getSymbol()).append("\n");
            if (holding.getTargetPrice3Month() > 0) {
                prompt.append("- Your 3M target: $").append(holding.getTargetPrice3Month()).append("\n");
            }
            if (holding.getTargetPriceLongTerm() > 0) {
                prompt.append("- Your long-term target: $").append(holding.getTargetPriceLongTerm()).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("Provide your analysis in this EXACT JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"bottom_line\": \"BUY/WAIT/DCA - One clear sentence with reasoning\",\n");
        prompt.append("  \"recommendation\": \"BUY/WAIT/DCA\",\n");
        prompt.append("  \"confidence\": \"HIGH/MEDIUM/LOW\",\n");
        prompt.append("  \"target_allocation\": \"Recommended portfolio percentage for ").append(holding.getSymbol()).append("\",\n");
        prompt.append("  \"current_price_analysis\": {\n");
        prompt.append("    \"entry_quality\": \"EXCELLENT/GOOD/FAIR/POOR\",\n");
        prompt.append("    \"price_context\": \"Analysis of current price relative to recent ranges\",\n");
        prompt.append("    \"value_assessment\": \"Whether ").append(holding.getSymbol()).append(" is undervalued, fairly valued, or overvalued\",\n");
        prompt.append("    \"timing_factors\": \"Key factors affecting entry timing\"\n");
        prompt.append("  },\n");
        prompt.append("  \"technical_levels\": {\n");
        prompt.append("    \"support_levels\": [\"$2800\", \"$2650\", \"$2400\"],\n");
        prompt.append("    \"resistance_levels\": [\"$3200\", \"$3400\", \"$3600\"],\n");
        prompt.append("    \"key_breakout_level\": \"Critical level for bullish momentum\",\n");
        prompt.append("    \"stop_loss_suggestion\": \"Recommended stop-loss level\"\n");
        prompt.append("  },\n");
        prompt.append("  \"outlook\": {\n");
        prompt.append("    \"short_term\": \"1-3 month outlook with key factors\",\n");
        prompt.append("    \"medium_term\": \"3-12 month outlook and expectations\",\n");
        prompt.append("    \"long_term\": \"1-3 year outlook and potential\"\n");
        prompt.append("  },\n");
        prompt.append("  \"strategy\": {\n");
        prompt.append("    \"entry_strategy\": \"Recommended approach for entering position\",\n");
        prompt.append("    \"dca_schedule\": \"If DCA recommended, suggested schedule\",\n");
        prompt.append("    \"profit_targets\": [\"First target\", \"Second target\", \"Long-term target\"],\n");
        prompt.append("    \"risk_management\": \"Key risk management considerations\"\n");
        prompt.append("  },\n");
        prompt.append("  \"market_sentiment\": {\n");
        prompt.append("    \"current_sentiment\": \"BULLISH/NEUTRAL/BEARISH\",\n");
        prompt.append("    \"institutional_activity\": \"Analysis of institutional interest\",\n");
        prompt.append("    \"retail_sentiment\": \"Retail investor sentiment analysis\",\n");
        prompt.append("    \"macro_factors\": \"Key macroeconomic factors affecting ").append(holding.getSymbol()).append("\"\n");
        prompt.append("  },\n");
        prompt.append("  \"fundamentals\": {\n");
        prompt.append("    \"network_health\": \"").append(holding.getName()).append(" network metrics and health\",\n");
        prompt.append("    \"staking_metrics\": \"").append(holding.getSymbol()).append(" staking data and implications (if applicable)\",\n");
        prompt.append("    \"ecosystem_activity\": \"Ecosystem activity and ").append(holding.getSymbol()).append(" demand\",\n");
        prompt.append("    \"development_activity\": \"").append(holding.getName()).append(" development progress\",\n");
        prompt.append("    \"competitive_position\": \"").append(holding.getSymbol()).append(" vs competitors in its sector\"\n");
        prompt.append("  },\n");
        prompt.append("  \"catalysts_and_risks\": {\n");
        prompt.append("    \"positive_catalysts\": [\"Bullish factor 1\", \"Bullish factor 2\", \"Bullish factor 3\"],\n");
        prompt.append("    \"risk_factors\": [\"Risk factor 1\", \"Risk factor 2\", \"Risk factor 3\"],\n");
        prompt.append("    \"upcoming_events\": [\"Important upcoming events affecting ").append(holding.getSymbol()).append("\"],\n");
        prompt.append("    \"regulatory_outlook\": \"Regulatory environment analysis\"\n");
        prompt.append("  },\n");
        prompt.append("  \"position_sizing\": {\n");
        prompt.append("    \"conservative_allocation\": \"Recommended % for conservative investors\",\n");
        prompt.append("    \"moderate_allocation\": \"Recommended % for moderate risk investors\",\n");
        prompt.append("    \"aggressive_allocation\": \"Recommended % for aggressive investors\",\n");
        prompt.append("    \"sizing_rationale\": \"Reasoning behind allocation recommendations\"\n");
        prompt.append("  },\n");
        prompt.append("  \"key_triggers\": {\n");
        prompt.append("    \"buy_triggers\": [\"Conditions that would trigger buy signal\"],\n");
        prompt.append("    \"sell_triggers\": [\"Conditions that would trigger sell signal\"],\n");
        prompt.append("    \"reassessment_triggers\": [\"Events that would require strategy reassessment\"]\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("Ensure your analysis is:\n");
        prompt.append("- Data-driven and objective\n");
        prompt.append("- Considers both technical and fundamental factors\n");
        prompt.append("- Includes specific price levels and percentages where applicable\n");
        prompt.append("- Provides actionable recommendations\n");
        prompt.append("- Acknowledges risks and uncertainties\n");
        prompt.append("- Suitable for different risk tolerance levels\n");
        prompt.append("- Tailored specifically to ").append(holding.getSymbol()).append(" and its unique characteristics");
        
        return prompt.toString();
    }

    private Map<String, Object> parseInvestmentAnalysisResponse(String response, String symbol) {
        Map<String, Object> parsedData = new HashMap<>();
        
        try {
            // Clean the response
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            
            // Find JSON content
            String jsonResponse = cleanResponse;
            int jsonStart = cleanResponse.indexOf("{");
            int jsonEnd = cleanResponse.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse = cleanResponse.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode responseNode = objectMapper.readTree(jsonResponse);
            
            // Parse top-level fields
            parsedData.put("symbol", symbol);
            parsedData.put("bottom_line", responseNode.has("bottom_line") ? responseNode.get("bottom_line").asText() : "WAIT - Analysis incomplete");
            parsedData.put("recommendation", responseNode.has("recommendation") ? responseNode.get("recommendation").asText() : "WAIT");
            parsedData.put("confidence", responseNode.has("confidence") ? responseNode.get("confidence").asText() : "LOW");
            parsedData.put("target_allocation", responseNode.has("target_allocation") ? responseNode.get("target_allocation").asText() : "5-10%");
            
            // Parse current price analysis
            if (responseNode.has("current_price_analysis")) {
                JsonNode priceNode = responseNode.get("current_price_analysis");
                Map<String, String> priceAnalysis = new HashMap<>();
                priceAnalysis.put("entry_quality", priceNode.has("entry_quality") ? priceNode.get("entry_quality").asText() : "FAIR");
                priceAnalysis.put("price_context", priceNode.has("price_context") ? priceNode.get("price_context").asText() : "Current price analysis pending");
                priceAnalysis.put("value_assessment", priceNode.has("value_assessment") ? priceNode.get("value_assessment").asText() : "Fairly valued");
                priceAnalysis.put("timing_factors", priceNode.has("timing_factors") ? priceNode.get("timing_factors").asText() : "Market timing considerations");
                parsedData.put("current_price_analysis", priceAnalysis);
            }
            
            // Parse technical levels
            if (responseNode.has("technical_levels")) {
                JsonNode techNode = responseNode.get("technical_levels");
                Map<String, Object> technicalLevels = new HashMap<>();
                
                // Parse support levels
                if (techNode.has("support_levels")) {
                    List<String> supportLevels = new ArrayList<>();
                    JsonNode supportArray = techNode.get("support_levels");
                    for (JsonNode level : supportArray) {
                        supportLevels.add(level.asText());
                    }
                    technicalLevels.put("support_levels", supportLevels);
                }
                
                // Parse resistance levels
                if (techNode.has("resistance_levels")) {
                    List<String> resistanceLevels = new ArrayList<>();
                    JsonNode resistanceArray = techNode.get("resistance_levels");
                    for (JsonNode level : resistanceArray) {
                        resistanceLevels.add(level.asText());
                    }
                    technicalLevels.put("resistance_levels", resistanceLevels);
                }
                
                technicalLevels.put("key_breakout_level", techNode.has("key_breakout_level") ? techNode.get("key_breakout_level").asText() : "TBD");
                technicalLevels.put("stop_loss_suggestion", techNode.has("stop_loss_suggestion") ? techNode.get("stop_loss_suggestion").asText() : "Set based on risk tolerance");
                parsedData.put("technical_levels", technicalLevels);
            }
            
            // Parse outlook
            if (responseNode.has("outlook")) {
                JsonNode outlookNode = responseNode.get("outlook");
                Map<String, String> outlook = new HashMap<>();
                outlook.put("short_term", outlookNode.has("short_term") ? outlookNode.get("short_term").asText() : "Short-term outlook pending");
                outlook.put("medium_term", outlookNode.has("medium_term") ? outlookNode.get("medium_term").asText() : "Medium-term outlook pending");
                outlook.put("long_term", outlookNode.has("long_term") ? outlookNode.get("long_term").asText() : "Long-term outlook pending");
                parsedData.put("outlook", outlook);
            }
            
            // Parse strategy
            if (responseNode.has("strategy")) {
                JsonNode strategyNode = responseNode.get("strategy");
                Map<String, Object> strategy = new HashMap<>();
                strategy.put("entry_strategy", strategyNode.has("entry_strategy") ? strategyNode.get("entry_strategy").asText() : "Gradual entry recommended");
                strategy.put("dca_schedule", strategyNode.has("dca_schedule") ? strategyNode.get("dca_schedule").asText() : "Weekly or bi-weekly");
                strategy.put("risk_management", strategyNode.has("risk_management") ? strategyNode.get("risk_management").asText() : "Standard risk management principles");
                
                // Parse profit targets
                if (strategyNode.has("profit_targets")) {
                    List<String> profitTargets = new ArrayList<>();
                    JsonNode targetsArray = strategyNode.get("profit_targets");
                    for (JsonNode target : targetsArray) {
                        profitTargets.add(target.asText());
                    }
                    strategy.put("profit_targets", profitTargets);
                }
                
                parsedData.put("strategy", strategy);
            }
            
            // Parse market sentiment
            if (responseNode.has("market_sentiment")) {
                JsonNode sentimentNode = responseNode.get("market_sentiment");
                Map<String, String> marketSentiment = new HashMap<>();
                marketSentiment.put("current_sentiment", sentimentNode.has("current_sentiment") ? sentimentNode.get("current_sentiment").asText() : "NEUTRAL");
                marketSentiment.put("institutional_activity", sentimentNode.has("institutional_activity") ? sentimentNode.get("institutional_activity").asText() : "Institutional activity analysis");
                marketSentiment.put("retail_sentiment", sentimentNode.has("retail_sentiment") ? sentimentNode.get("retail_sentiment").asText() : "Retail sentiment analysis");
                marketSentiment.put("macro_factors", sentimentNode.has("macro_factors") ? sentimentNode.get("macro_factors").asText() : "Macro factors analysis");
                parsedData.put("market_sentiment", marketSentiment);
            }
            
            // Parse fundamentals
            if (responseNode.has("fundamentals")) {
                JsonNode fundamentalsNode = responseNode.get("fundamentals");
                Map<String, String> fundamentals = new HashMap<>();
                fundamentals.put("network_health", fundamentalsNode.has("network_health") ? fundamentalsNode.get("network_health").asText() : "Network health analysis");
                fundamentals.put("staking_metrics", fundamentalsNode.has("staking_metrics") ? fundamentalsNode.get("staking_metrics").asText() : "Staking metrics analysis");
                fundamentals.put("ecosystem_activity", fundamentalsNode.has("ecosystem_activity") ? fundamentalsNode.get("ecosystem_activity").asText() : "Ecosystem activity analysis");
                fundamentals.put("development_activity", fundamentalsNode.has("development_activity") ? fundamentalsNode.get("development_activity").asText() : "Development activity analysis");
                fundamentals.put("competitive_position", fundamentalsNode.has("competitive_position") ? fundamentalsNode.get("competitive_position").asText() : "Competitive position analysis");
                parsedData.put("fundamentals", fundamentals);
            }
            
            // Parse catalysts and risks
            if (responseNode.has("catalysts_and_risks")) {
                JsonNode catalystsNode = responseNode.get("catalysts_and_risks");
                Map<String, Object> catalystsRisks = new HashMap<>();
                
                // Parse positive catalysts
                if (catalystsNode.has("positive_catalysts")) {
                    List<String> positiveCatalysts = new ArrayList<>();
                    JsonNode catalystsArray = catalystsNode.get("positive_catalysts");
                    for (JsonNode catalyst : catalystsArray) {
                        positiveCatalysts.add(catalyst.asText());
                    }
                    catalystsRisks.put("positive_catalysts", positiveCatalysts);
                }
                
                // Parse risk factors
                if (catalystsNode.has("risk_factors")) {
                    List<String> riskFactors = new ArrayList<>();
                    JsonNode risksArray = catalystsNode.get("risk_factors");
                    for (JsonNode risk : risksArray) {
                        riskFactors.add(risk.asText());
                    }
                    catalystsRisks.put("risk_factors", riskFactors);
                }
                
                // Parse upcoming events
                if (catalystsNode.has("upcoming_events")) {
                    List<String> upcomingEvents = new ArrayList<>();
                    JsonNode eventsArray = catalystsNode.get("upcoming_events");
                    for (JsonNode event : eventsArray) {
                        upcomingEvents.add(event.asText());
                    }
                    catalystsRisks.put("upcoming_events", upcomingEvents);
                }
                
                catalystsRisks.put("regulatory_outlook", catalystsNode.has("regulatory_outlook") ? catalystsNode.get("regulatory_outlook").asText() : "Regulatory outlook analysis");
                parsedData.put("catalysts_and_risks", catalystsRisks);
            }
            
            // Parse position sizing
            if (responseNode.has("position_sizing")) {
                JsonNode positionNode = responseNode.get("position_sizing");
                Map<String, String> positionSizing = new HashMap<>();
                positionSizing.put("conservative_allocation", positionNode.has("conservative_allocation") ? positionNode.get("conservative_allocation").asText() : "5-10%");
                positionSizing.put("moderate_allocation", positionNode.has("moderate_allocation") ? positionNode.get("moderate_allocation").asText() : "10-20%");
                positionSizing.put("aggressive_allocation", positionNode.has("aggressive_allocation") ? positionNode.get("aggressive_allocation").asText() : "20-30%");
                positionSizing.put("sizing_rationale", positionNode.has("sizing_rationale") ? positionNode.get("sizing_rationale").asText() : "Allocation rationale");
                parsedData.put("position_sizing", positionSizing);
            }
            
            // Parse key triggers
            if (responseNode.has("key_triggers")) {
                JsonNode triggersNode = responseNode.get("key_triggers");
                Map<String, Object> keyTriggers = new HashMap<>();
                
                // Parse buy triggers
                if (triggersNode.has("buy_triggers")) {
                    List<String> buyTriggers = new ArrayList<>();
                    JsonNode buyArray = triggersNode.get("buy_triggers");
                    for (JsonNode trigger : buyArray) {
                        buyTriggers.add(trigger.asText());
                    }
                    keyTriggers.put("buy_triggers", buyTriggers);
                }
                
                // Parse sell triggers
                if (triggersNode.has("sell_triggers")) {
                    List<String> sellTriggers = new ArrayList<>();
                    JsonNode sellArray = triggersNode.get("sell_triggers");
                    for (JsonNode trigger : sellArray) {
                        sellTriggers.add(trigger.asText());
                    }
                    keyTriggers.put("sell_triggers", sellTriggers);
                }
                
                // Parse reassessment triggers
                if (triggersNode.has("reassessment_triggers")) {
                    List<String> reassessmentTriggers = new ArrayList<>();
                    JsonNode reassessmentArray = triggersNode.get("reassessment_triggers");
                    for (JsonNode trigger : reassessmentArray) {
                        reassessmentTriggers.add(trigger.asText());
                    }
                    keyTriggers.put("reassessment_triggers", reassessmentTriggers);
                }
                
                parsedData.put("key_triggers", keyTriggers);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing investment analysis response for {}: ", symbol, e);
            // Return default structured response
            parsedData.put("symbol", symbol);
            parsedData.put("bottom_line", "WAIT - Unable to parse analysis response");
            parsedData.put("recommendation", "WAIT");
            parsedData.put("confidence", "LOW");
            parsedData.put("current_price_analysis", new HashMap<>());
            parsedData.put("technical_levels", new HashMap<>());
            parsedData.put("outlook", new HashMap<>());
            parsedData.put("strategy", new HashMap<>());
            parsedData.put("market_sentiment", new HashMap<>());
            parsedData.put("fundamentals", new HashMap<>());
            parsedData.put("catalysts_and_risks", new HashMap<>());
            parsedData.put("position_sizing", new HashMap<>());
            parsedData.put("key_triggers", new HashMap<>());
        }
        
        return parsedData;
    }

    private String buildEntryExitStrategyPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Entry & Exit Strategy Analysis Prompt\n");
        prompt.append("Provide comprehensive entry and exit strategy recommendations for my cryptocurrency portfolio holdings.\n\n");

        prompt.append("For each cryptocurrency, provide:\n");
        prompt.append("1. Entry Strategy Analysis - Optimal entry points, market timing, DCA strategy, risk management\n");
        prompt.append("2. Exit Strategy Analysis - Profit-taking levels, stop-loss strategy, time-based exits, market condition exits\n");
        prompt.append("3. Market Context Analysis - Current market phase, sector rotation, correlation analysis, risk assessment\n");
        prompt.append("4. Strategic Recommendations - Hold vs. trade, allocation suggestions, hedging strategies, timeline considerations\n\n");

        prompt.append("Portfolio Holdings with Current Market Data:\n\n");

        // Add detailed portfolio information WITH current market data
        for (Holding holding : holdings) {
            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Initial Investment Value: $%.2f\n", holding.getInitialValue()));

            // Fetch and include current market data for each holding
            try {
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    prompt.append("\n--- Current Market Data ---\n");
                    prompt.append(String.format("Current Price: $%.2f\n", marketData.getCurrentPrice()));
                    prompt.append(String.format("24h Change: %.2f%%\n", marketData.getPriceChangePercentage24h()));
                    prompt.append(String.format("24h Volume: $%.0f\n", marketData.getVolume24h()));

                    // Calculate current P&L for context
                    double currentValue = holding.getHoldings() * marketData.getCurrentPrice();
                    double profitLoss = currentValue - holding.getInitialValue();
                    double profitLossPercentage = (profitLoss / holding.getInitialValue()) * 100;
                    prompt.append(String.format("Current Position Value: $%.2f\n", currentValue));
                    prompt.append(String.format("Unrealized P&L: $%.2f (%.1f%%)\n", profitLoss, profitLossPercentage));

                    // Include technical indicators if available
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

                    // Price context relative to moving averages
                    double currentPrice = marketData.getCurrentPrice();
                    if (marketData.getSma20() != null) {
                        double sma20Distance = ((currentPrice - marketData.getSma20()) / marketData.getSma20()) * 100;
                        prompt.append(String.format("Distance from SMA20: %.1f%%\n", sma20Distance));
                    }
                    if (marketData.getSma50() != null) {
                        double sma50Distance = ((currentPrice - marketData.getSma50()) / marketData.getSma50()) * 100;
                        prompt.append(String.format("Distance from SMA50: %.1f%%\n", sma50Distance));
                    }
                }
            } catch (Exception e) {
                prompt.append("\n--- Market Data Unavailable ---\n");
                System.err.println("Could not fetch market data for " + holding.getSymbol() + ": " + e.getMessage());
            }

            prompt.append("\n");
        }

        // --- Context & Preferences ---
        prompt.append("⚠️ Context & Preferences:\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());

        // --- Output Format ---
        prompt.append("👉 Please provide your analysis in the following structured JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"strategies\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"BTC\",\n");
        prompt.append("      \"action\": \"BUY_NOW|WAIT|HOLD|TRIM\",\n");
        prompt.append("      \"entry\": {\n");
        prompt.append("        \"zones\": [\"$xxxxx\", \"$yyyyy\", \"$zzzzz\"],\n");
        prompt.append("        \"dca\": \"frequency + amount/percent (optional)\",\n");
        prompt.append("        \"confirmation\": \"e.g., reclaim 200D MA; RSI cross; structure break\"\n");
        prompt.append("      },\n");
        prompt.append("      \"exit\": {\n");
        prompt.append("        \"take_profits\": [\n");
        prompt.append("          {\"level\": \"$tp1\", \"sell_pct\": \"25%\"},\n");
        prompt.append("          {\"level\": \"$tp2\", \"sell_pct\": \"25%\"},\n");
        prompt.append("          {\"level\": \"$tp3\", \"sell_pct\": \"50%\"}\n");
        prompt.append("        ],\n");
        prompt.append("        \"stop\": {\"type\": \"hard\", \"value\": \"$or-%%\"},\n");
        prompt.append("        \"trailing\": {\"type\": \"percent|ATR\", \"value\": \"e.g., 10%\"},\n");
        prompt.append("        \"invalidations\": [\"one clear invalidation condition\"]\n");
        prompt.append("      },\n");
        prompt.append("      \"notes\": \"<= 2 short lines: key catalyst/risks impacting entries/exits\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"portfolio\": {\n");
        prompt.append("    \"cash_buffer\": \"suggested % for new entries\",\n");
        prompt.append("    \"rebalance_trigger\": \"e.g., +/-5% drift or TP hit\",\n");
        prompt.append("    \"risk_guardrails\": \"max position %; max portfolio drawdown rule\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private Map<String, Object> parseEntryExitStrategyResponse(String response) {
        Map<String, Object> parsed = new HashMap<>();

        try {
            System.out.println("DEBUG: Raw AI response: " + response);
            
            // Strip code fences if present
            String clean = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

            // Extract the JSON block
            int start = clean.indexOf("{");
            int end = clean.lastIndexOf("}");
            String json = (start >= 0 && end > start) ? clean.substring(start, end + 1) : clean;
            
            System.out.println("DEBUG: Extracted JSON: " + json);

            JsonNode root = objectMapper.readTree(json);

            // === strategies ===
            List<Map<String, Object>> strategies = new ArrayList<>();
            if (root.has("strategies") && root.get("strategies").isArray()) {
                for (JsonNode s : root.get("strategies")) {
                    Map<String, Object> one = new HashMap<>();
                    one.put("symbol", s.has("symbol") ? s.get("symbol").asText() : "");
                    one.put("action", s.has("action") ? s.get("action").asText() : "");

                    // entry
                    Map<String, Object> entry = new HashMap<>();
                    if (s.has("entry") && s.get("entry").isObject()) {
                        JsonNode e = s.get("entry");
                        // zones
                        List<String> zones = new ArrayList<>();
                        if (e.has("zones") && e.get("zones").isArray()) {
                            for (JsonNode z : e.get("zones")) zones.add(z.asText());
                        }
                        entry.put("zones", zones);
                        entry.put("dca", e.has("dca") ? e.get("dca").asText() : "");
                        entry.put("confirmation", e.has("confirmation") ? e.get("confirmation").asText() : "");
                    }
                    one.put("entry", entry);

                    // exit
                    Map<String, Object> exit = new HashMap<>();
                    if (s.has("exit") && s.get("exit").isObject()) {
                        JsonNode x = s.get("exit");

                        // take_profits
                        List<Map<String, String>> tps = new ArrayList<>();
                        if (x.has("take_profits") && x.get("take_profits").isArray()) {
                            for (JsonNode tp : x.get("take_profits")) {
                                Map<String, String> tpMap = new HashMap<>();
                                tpMap.put("level", tp.has("level") ? tp.get("level").asText() : "");
                                tpMap.put("sell_pct", tp.has("sell_pct") ? tp.get("sell_pct").asText() : "");
                                tps.add(tpMap);
                            }
                        }
                        exit.put("take_profits", tps);

                        // stop
                        Map<String, String> stop = new HashMap<>();
                        if (x.has("stop") && x.get("stop").isObject()) {
                            JsonNode st = x.get("stop");
                            stop.put("type", st.has("type") ? st.get("type").asText() : "");
                            stop.put("value", st.has("value") ? st.get("value").asText() : "");
                        }
                        exit.put("stop", stop);

                        // trailing
                        Map<String, String> trailing = new HashMap<>();
                        if (x.has("trailing") && x.get("trailing").isObject()) {
                            JsonNode tr = x.get("trailing");
                            trailing.put("type", tr.has("type") ? tr.get("type").asText() : "");
                            trailing.put("value", tr.has("value") ? tr.get("value").asText() : "");
                        }
                        exit.put("trailing", trailing);

                        // invalidations
                        List<String> invalidations = new ArrayList<>();
                        if (x.has("invalidations") && x.get("invalidations").isArray()) {
                            for (JsonNode inv : x.get("invalidations")) invalidations.add(inv.asText());
                        }
                        exit.put("invalidations", invalidations);
                    }
                    one.put("exit", exit);

                    one.put("notes", s.has("notes") ? s.get("notes").asText() : "");
                    strategies.add(one);
                }
            }
            parsed.put("strategies", strategies);

            // === portfolio ===
            Map<String, String> portfolio = new HashMap<>();
            if (root.has("portfolio") && root.get("portfolio").isObject()) {
                JsonNode p = root.get("portfolio");
                portfolio.put("cash_buffer", p.has("cash_buffer") ? p.get("cash_buffer").asText() : "");
                portfolio.put("rebalance_trigger", p.has("rebalance_trigger") ? p.get("rebalance_trigger").asText() : "");
                portfolio.put("risk_guardrails", p.has("risk_guardrails") ? p.get("risk_guardrails").asText() : "");
            }
            parsed.put("portfolio", portfolio);

            // Optional: minimal backward-compat shim (old responses)
            if (strategies.isEmpty() && root.has("individual_strategies")) {
                // Populate empty keys so downstream code doesn't break
                parsed.putIfAbsent("portfolio", portfolio);
            }

        } catch (Exception e) {
            System.err.println("Error parsing entry/exit strategy response: " + e.getMessage());
            e.printStackTrace();

            // Safe defaults on failure
            parsed.put("strategies", new ArrayList<>());
            parsed.put("portfolio", new HashMap<>());
        }

        System.out.println("DEBUG: Final parsed data: " + parsed);
        System.out.println("DEBUG: Contains strategies key: " + parsed.containsKey("strategies"));
        if (parsed.containsKey("strategies")) {
            System.out.println("DEBUG: Strategies value: " + parsed.get("strategies"));
        }
        return parsed;
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
                double initialValue = holding.getInitialValue();
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
            double positionValue = holding.getInitialValue();
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
                .mapToDouble(Holding::getInitialValue)
                .sum();
    }
    private String buildUSDTAllocationPrompt(List<Holding> holdings, double usdtAmount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("USDT Allocation Strategy Analysis Prompt\n");
        prompt.append("I currently hold USDT in my portfolio and need actionable suggestions for how to best use this capital.\n\n");

        prompt.append(String.format("Available USDT for deployment: $%.2f\n\n", usdtAmount));

        prompt.append("Current Portfolio Overview:\n");

        // Calculate total portfolio value for context
        double totalPortfolioValue = 0;
        for (Holding holding : holdings) {
            totalPortfolioValue += holding.getInitialValue();
        }

        // Add detailed portfolio information
        for (Holding holding : holdings) {
            double currentWeight = (holding.getInitialValue() / totalPortfolioValue) * 100;

            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Initial Investment: $%.2f (%.1f%% of portfolio)\n", holding.getInitialValue(), currentWeight));
            prompt.append(String.format("3-Month Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("Long-term Target: $%.2f\n", holding.getTargetPriceLongTerm()));
            prompt.append("\n");
        }

        // Add market data context for key holdings where possible
        prompt.append("--- Current Market Context ---\n");
        try {
            for (Holding holding : holdings) {
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    double currentPrice = marketData.getCurrentPrice();
                    double priceChange24h = marketData.getPriceChangePercentage24h();
                    prompt.append(String.format("%s: $%.2f (%.2f%% 24h)\n", holding.getSymbol(), currentPrice, priceChange24h));
                }
            }
        } catch (Exception e) {
            prompt.append("Market data temporarily unavailable for analysis\n");
        }
        prompt.append("\n");

        prompt.append("--- Investment Context & Preferences ---\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());
        prompt.append("- Exclusions: No meme coins or highly speculative low-cap tokens\n\n");

        prompt.append("--- Analysis Requirements ---\n");
        prompt.append("Please provide insights on:\n");
        prompt.append("1. How much of my USDT should I keep as a safety buffer (stablecoin reserve for future dips) vs. how much should be deployed now?\n");
        prompt.append("2. Which existing portfolio assets should I increase exposure to with part of my USDT?\n");
        prompt.append("3. Which new sectors or coins (AI, DeFi, interoperability, RWA, staking, decentralized infra) are worth entering with a portion of my USDT?\n");
        prompt.append("4. Give me specific allocation suggestions (e.g., 40% keep in USDT, 30% into ETH, 20% into RWA, 10% into AI infra)\n");
        prompt.append("5. Highlight if now is a good time to deploy capital immediately or if I should keep more stablecoins and wait for better entry opportunities\n\n");
        prompt.append("6. Ensure that the suggested USDT allocation is realistic, taking into account my current USDT holdings and the total value of my existing investments.\n\n");

        prompt.append("--- Output Format ---\n");
        prompt.append("Please structure your analysis in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"deployment_recommendation\": \"DEPLOY_NOW|WAIT_FOR_DIP|GRADUAL_DEPLOYMENT|MIXED_APPROACH\",\n");
        prompt.append("  \"market_timing_assessment\": \"Current market conditions and timing considerations\",\n");
        prompt.append("  \"reserve_strategy\": {\n");
        prompt.append("    \"recommended_reserve_percentage\": \"30-40%\",\n");
        prompt.append("    \"reserve_amount\": \"$15,000\",\n");
        prompt.append("    \"reasoning\": \"Why this reserve level is appropriate\",\n");
        prompt.append("    \"deployment_triggers\": [\"Market conditions that would trigger reserve deployment\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"existing_positions_enhancement\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"ETH\",\n");
        prompt.append("      \"allocation_percentage\": \"25%\",\n");
        prompt.append("      \"allocation_amount\": \"$10,000\",\n");
        prompt.append("      \"current_weight_in_portfolio\": \"15%\",\n");
        prompt.append("      \"target_weight_after_addition\": \"20%\",\n");
        prompt.append("      \"rationale\": \"Why increasing this position makes sense\",\n");
        prompt.append("      \"entry_strategy\": \"DCA over 4 weeks or lump sum on dips\",\n");
        prompt.append("      \"target_entry_price\": \"$3,200-3,400 range\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"new_opportunities\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"sector\": \"Real World Assets (RWA)\",\n");
        prompt.append("      \"allocation_percentage\": \"15%\",\n");
        prompt.append("      \"allocation_amount\": \"$6,000\",\n");
        prompt.append("      \"suggested_assets\": [\"ONDO\", \"RWA\", \"BUIDL\"],\n");
        prompt.append("      \"primary_recommendation\": \"ONDO\",\n");
        prompt.append("      \"rationale\": \"Why this sector/asset is attractive now\",\n");
        prompt.append("      \"risk_level\": \"LOW|MEDIUM|HIGH\",\n");
        prompt.append("      \"entry_strategy\": \"Specific entry approach\",\n");
        prompt.append("      \"target_allocation_in_portfolio\": \"3-5%\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"allocation_breakdown\": {\n");
        prompt.append("    \"keep_as_usdt_reserve\": {\n");
        prompt.append("      \"percentage\": \"35%\",\n");
        prompt.append("      \"amount\": \"$14,000\",\n");
        prompt.append("      \"purpose\": \"Safety buffer and opportunity fund\"\n");
        prompt.append("    },\n");
        prompt.append("    \"add_to_existing_positions\": {\n");
        prompt.append("      \"percentage\": \"40%\",\n");
        prompt.append("      \"amount\": \"$16,000\",\n");
        prompt.append("      \"breakdown\": [\n");
        prompt.append("        {\"symbol\": \"ETH\", \"percentage\": \"25%\", \"amount\": \"$10,000\"},\n");
        prompt.append("        {\"symbol\": \"SOL\", \"percentage\": \"15%\", \"amount\": \"$6,000\"}\n");
        prompt.append("      ]\n");
        prompt.append("    },\n");
        prompt.append("    \"new_positions\": {\n");
        prompt.append("      \"percentage\": \"25%\",\n");
        prompt.append("      \"amount\": \"$10,000\",\n");
        prompt.append("      \"breakdown\": [\n");
        prompt.append("        {\"sector\": \"AI Infrastructure\", \"percentage\": \"10%\", \"amount\": \"$4,000\"},\n");
        prompt.append("        {\"sector\": \"RWA\", \"percentage\": \"15%\", \"amount\": \"$6,000\"}\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  },\n");
        prompt.append("  \"execution_timeline\": {\n");
        prompt.append("    \"immediate_actions\": [\"Actions to take within 1-2 weeks\"],\n");
        prompt.append("    \"short_term_actions\": [\"Actions for next 1-3 months\"],\n");
        prompt.append("    \"contingency_plans\": [\"What to do if market conditions change\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"risk_considerations\": {\n");
        prompt.append("    \"main_risks\": [\"Key risks to monitor\"],\n");
        prompt.append("    \"risk_mitigation_strategies\": [\"How to manage identified risks\"],\n");
        prompt.append("    \"portfolio_impact\": \"How these changes will affect overall portfolio risk/return profile\"\n");
        prompt.append("  },\n");
        prompt.append("  \"key_recommendations\": [\n");
        prompt.append("    \"Top 3-5 actionable recommendations with specific dollar amounts and timelines\"\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("Ensure your recommendations are:\n");
        prompt.append("- Specific with exact dollar amounts and percentages\n");
        prompt.append("- Tailored to current market conditions\n");
        prompt.append("- Aligned with moderate risk tolerance and 6 months to 3 years timeframe\n");
        prompt.append("- Focused on projects with strong fundamentals and real-world utility\n");
        prompt.append("- Considerate of portfolio diversification and risk management\n");
        prompt.append("- Actionable with clear next steps and timing\n");

        return prompt.toString();
    }

    private Map<String, Object> parseUSDTAllocationResponse(String response) {
        Map<String, Object> parsedData = new HashMap<>();

        try {
            // Remove markdown code blocks if present
            String cleanResponse = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

            // Extract JSON from response (in case there's extra text)
            String jsonResponse = cleanResponse;
            int jsonStart = cleanResponse.indexOf("{");
            int jsonEnd = cleanResponse.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse = cleanResponse.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode responseNode = objectMapper.readTree(jsonResponse);

            // Parse top-level fields
            parsedData.put("deployment_recommendation", responseNode.has("deployment_recommendation") ? responseNode.get("deployment_recommendation").asText() : "GRADUAL_DEPLOYMENT");
            parsedData.put("market_timing_assessment", responseNode.has("market_timing_assessment") ? responseNode.get("market_timing_assessment").asText() : "Market conditions assessment pending");

            // Parse reserve strategy
            if (responseNode.has("reserve_strategy")) {
                JsonNode reserveNode = responseNode.get("reserve_strategy");
                Map<String, Object> reserveStrategy = new HashMap<>();

                reserveStrategy.put("recommended_reserve_percentage", reserveNode.has("recommended_reserve_percentage") ? reserveNode.get("recommended_reserve_percentage").asText() : "30-40%");
                reserveStrategy.put("reserve_amount", reserveNode.has("reserve_amount") ? reserveNode.get("reserve_amount").asText() : "TBD");
                reserveStrategy.put("reasoning", reserveNode.has("reasoning") ? reserveNode.get("reasoning").asText() : "Reserve reasoning pending");

                // Parse deployment triggers
                if (reserveNode.has("deployment_triggers")) {
                    List<String> triggers = new ArrayList<>();
                    JsonNode triggersArray = reserveNode.get("deployment_triggers");
                    for (JsonNode trigger : triggersArray) {
                        triggers.add(trigger.asText());
                    }
                    reserveStrategy.put("deployment_triggers", triggers);
                } else {
                    reserveStrategy.put("deployment_triggers", new ArrayList<>());
                }

                parsedData.put("reserve_strategy", reserveStrategy);
            }

            // Parse existing positions enhancement
            if (responseNode.has("existing_positions_enhancement")) {
                List<Map<String, String>> existingEnhancements = new ArrayList<>();
                JsonNode enhancementArray = responseNode.get("existing_positions_enhancement");

                for (JsonNode enhancement : enhancementArray) {
                    Map<String, String> enhancementData = new HashMap<>();
                    enhancementData.put("symbol", enhancement.has("symbol") ? enhancement.get("symbol").asText() : "");
                    enhancementData.put("allocation_percentage", enhancement.has("allocation_percentage") ? enhancement.get("allocation_percentage").asText() : "0%");
                    enhancementData.put("allocation_amount", enhancement.has("allocation_amount") ? enhancement.get("allocation_amount").asText() : "$0");
                    enhancementData.put("current_weight_in_portfolio", enhancement.has("current_weight_in_portfolio") ? enhancement.get("current_weight_in_portfolio").asText() : "0%");
                    enhancementData.put("target_weight_after_addition", enhancement.has("target_weight_after_addition") ? enhancement.get("target_weight_after_addition").asText() : "0%");
                    enhancementData.put("rationale", enhancement.has("rationale") ? enhancement.get("rationale").asText() : "Enhancement rationale");
                    enhancementData.put("entry_strategy", enhancement.has("entry_strategy") ? enhancement.get("entry_strategy").asText() : "Gradual entry");
                    enhancementData.put("target_entry_price", enhancement.has("target_entry_price") ? enhancement.get("target_entry_price").asText() : "Market price");
                    existingEnhancements.add(enhancementData);
                }
                parsedData.put("existing_positions_enhancement", existingEnhancements);
            }

            // Parse new opportunities
            if (responseNode.has("new_opportunities")) {
                List<Map<String, Object>> newOpportunities = new ArrayList<>();
                JsonNode opportunitiesArray = responseNode.get("new_opportunities");

                for (JsonNode opportunity : opportunitiesArray) {
                    Map<String, Object> opportunityData = new HashMap<>();
                    opportunityData.put("sector", opportunity.has("sector") ? opportunity.get("sector").asText() : "");
                    opportunityData.put("allocation_percentage", opportunity.has("allocation_percentage") ? opportunity.get("allocation_percentage").asText() : "0%");
                    opportunityData.put("allocation_amount", opportunity.has("allocation_amount") ? opportunity.get("allocation_amount").asText() : "$0");
                    opportunityData.put("primary_recommendation", opportunity.has("primary_recommendation") ? opportunity.get("primary_recommendation").asText() : "");
                    opportunityData.put("rationale", opportunity.has("rationale") ? opportunity.get("rationale").asText() : "Opportunity rationale");
                    opportunityData.put("risk_level", opportunity.has("risk_level") ? opportunity.get("risk_level").asText() : "MEDIUM");
                    opportunityData.put("entry_strategy", opportunity.has("entry_strategy") ? opportunity.get("entry_strategy").asText() : "Gradual entry");
                    opportunityData.put("target_allocation_in_portfolio", opportunity.has("target_allocation_in_portfolio") ? opportunity.get("target_allocation_in_portfolio").asText() : "3-5%");

                    // Parse suggested assets
                    if (opportunity.has("suggested_assets")) {
                        List<String> suggestedAssets = new ArrayList<>();
                        JsonNode assetsArray = opportunity.get("suggested_assets");
                        for (JsonNode asset : assetsArray) {
                            suggestedAssets.add(asset.asText());
                        }
                        opportunityData.put("suggested_assets", suggestedAssets);
                    } else {
                        opportunityData.put("suggested_assets", new ArrayList<>());
                    }

                    newOpportunities.add(opportunityData);
                }
                parsedData.put("new_opportunities", newOpportunities);
            }

            // Parse allocation breakdown
            if (responseNode.has("allocation_breakdown")) {
                JsonNode allocationNode = responseNode.get("allocation_breakdown");
                Map<String, Object> allocationBreakdown = new HashMap<>();

                // Parse USDT reserve
                if (allocationNode.has("keep_as_usdt_reserve")) {
                    JsonNode reserveNode = allocationNode.get("keep_as_usdt_reserve");
                    Map<String, String> usdtReserve = new HashMap<>();
                    usdtReserve.put("percentage", reserveNode.has("percentage") ? reserveNode.get("percentage").asText() : "0%");
                    usdtReserve.put("amount", reserveNode.has("amount") ? reserveNode.get("amount").asText() : "$0");
                    usdtReserve.put("purpose", reserveNode.has("purpose") ? reserveNode.get("purpose").asText() : "Safety buffer");
                    allocationBreakdown.put("keep_as_usdt_reserve", usdtReserve);
                }

                // Parse existing positions additions
                if (allocationNode.has("add_to_existing_positions")) {
                    JsonNode existingNode = allocationNode.get("add_to_existing_positions");
                    Map<String, Object> existingPositions = new HashMap<>();
                    existingPositions.put("percentage", existingNode.has("percentage") ? existingNode.get("percentage").asText() : "0%");
                    existingPositions.put("amount", existingNode.has("amount") ? existingNode.get("amount").asText() : "$0");

                    // Parse breakdown
                    if (existingNode.has("breakdown")) {
                        List<Map<String, String>> breakdown = new ArrayList<>();
                        JsonNode breakdownArray = existingNode.get("breakdown");
                        for (JsonNode item : breakdownArray) {
                            Map<String, String> itemData = new HashMap<>();
                            itemData.put("symbol", item.has("symbol") ? item.get("symbol").asText() : "");
                            itemData.put("percentage", item.has("percentage") ? item.get("percentage").asText() : "0%");
                            itemData.put("amount", item.has("amount") ? item.get("amount").asText() : "$0");
                            breakdown.add(itemData);
                        }
                        existingPositions.put("breakdown", breakdown);
                    }

                    allocationBreakdown.put("add_to_existing_positions", existingPositions);
                }

                // Parse new positions
                if (allocationNode.has("new_positions")) {
                    JsonNode newNode = allocationNode.get("new_positions");
                    Map<String, Object> newPositions = new HashMap<>();
                    newPositions.put("percentage", newNode.has("percentage") ? newNode.get("percentage").asText() : "0%");
                    newPositions.put("amount", newNode.has("amount") ? newNode.get("amount").asText() : "$0");

                    // Parse breakdown
                    if (newNode.has("breakdown")) {
                        List<Map<String, String>> breakdown = new ArrayList<>();
                        JsonNode breakdownArray = newNode.get("breakdown");
                        for (JsonNode item : breakdownArray) {
                            Map<String, String> itemData = new HashMap<>();
                            itemData.put("sector", item.has("sector") ? item.get("sector").asText() : "");
                            itemData.put("percentage", item.has("percentage") ? item.get("percentage").asText() : "0%");
                            itemData.put("amount", item.has("amount") ? item.get("amount").asText() : "$0");
                            breakdown.add(itemData);
                        }
                        newPositions.put("breakdown", breakdown);
                    }

                    allocationBreakdown.put("new_positions", newPositions);
                }

                parsedData.put("allocation_breakdown", allocationBreakdown);
            }

            // Parse execution timeline
            if (responseNode.has("execution_timeline")) {
                JsonNode timelineNode = responseNode.get("execution_timeline");
                Map<String, Object> timeline = new HashMap<>();

                // Parse immediate actions
                if (timelineNode.has("immediate_actions")) {
                    List<String> immediateActions = new ArrayList<>();
                    JsonNode actionsArray = timelineNode.get("immediate_actions");
                    for (JsonNode action : actionsArray) {
                        immediateActions.add(action.asText());
                    }
                    timeline.put("immediate_actions", immediateActions);
                }

                // Parse short term actions
                if (timelineNode.has("short_term_actions")) {
                    List<String> shortTermActions = new ArrayList<>();
                    JsonNode actionsArray = timelineNode.get("short_term_actions");
                    for (JsonNode action : actionsArray) {
                        shortTermActions.add(action.asText());
                    }
                    timeline.put("short_term_actions", shortTermActions);
                }

                // Parse contingency plans
                if (timelineNode.has("contingency_plans")) {
                    List<String> contingencyPlans = new ArrayList<>();
                    JsonNode plansArray = timelineNode.get("contingency_plans");
                    for (JsonNode plan : plansArray) {
                        contingencyPlans.add(plan.asText());
                    }
                    timeline.put("contingency_plans", contingencyPlans);
                }

                parsedData.put("execution_timeline", timeline);
            }

            // Parse risk considerations
            if (responseNode.has("risk_considerations")) {
                JsonNode riskNode = responseNode.get("risk_considerations");
                Map<String, Object> riskConsiderations = new HashMap<>();

                // Parse main risks
                if (riskNode.has("main_risks")) {
                    List<String> mainRisks = new ArrayList<>();
                    JsonNode risksArray = riskNode.get("main_risks");
                    for (JsonNode risk : risksArray) {
                        mainRisks.add(risk.asText());
                    }
                    riskConsiderations.put("main_risks", mainRisks);
                }

                // Parse risk mitigation strategies
                if (riskNode.has("risk_mitigation_strategies")) {
                    List<String> mitigationStrategies = new ArrayList<>();
                    JsonNode strategiesArray = riskNode.get("risk_mitigation_strategies");
                    for (JsonNode strategy : strategiesArray) {
                        mitigationStrategies.add(strategy.asText());
                    }
                    riskConsiderations.put("risk_mitigation_strategies", mitigationStrategies);
                }

                riskConsiderations.put("portfolio_impact", riskNode.has("portfolio_impact") ? riskNode.get("portfolio_impact").asText() : "Portfolio impact assessment");
                parsedData.put("risk_considerations", riskConsiderations);
            }

            // Parse key recommendations
            if (responseNode.has("key_recommendations")) {
                List<String> keyRecommendations = new ArrayList<>();
                JsonNode recommendationsArray = responseNode.get("key_recommendations");
                for (JsonNode recommendation : recommendationsArray) {
                    keyRecommendations.add(recommendation.asText());
                }
                parsedData.put("key_recommendations", keyRecommendations);
            }

        } catch (Exception e) {
            System.err.println("Error parsing USDT allocation response: " + e.getMessage());
            e.printStackTrace();

            // Set default values if parsing fails
            parsedData.put("deployment_recommendation", "GRADUAL_DEPLOYMENT");
            parsedData.put("market_timing_assessment", "Unable to parse market timing assessment");
            parsedData.put("reserve_strategy", new HashMap<>());
            parsedData.put("existing_positions_enhancement", new ArrayList<>());
            parsedData.put("new_opportunities", new ArrayList<>());
            parsedData.put("allocation_breakdown", new HashMap<>());
            parsedData.put("execution_timeline", new HashMap<>());
            parsedData.put("risk_considerations", new HashMap<>());
            parsedData.put("key_recommendations", new ArrayList<>());
        }

        return parsedData;
    }
}
