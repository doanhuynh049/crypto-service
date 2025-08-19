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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AdvisoryEngineService {

    @Autowired
    private AppConfig appConfig;

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
        String prompt = buildRiskOpportunityPrompt(holdings);
        System.out.println("Risk & Opportunity Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Risk & Opportunity Analysis AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parseRiskOpportunityResponse(aiResponse);
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

    private String buildRiskOpportunityPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Risk & Opportunity Analysis Prompt\n");
        prompt.append("Analyze my crypto portfolio below. For each coin, provide:\n");
        prompt.append("1. Current market outlook (short-term vs long-term).\n");
        prompt.append("2. Risk factors (volatility, liquidity, fundamentals).\n");
        prompt.append("3. Upside opportunities.\n");
        prompt.append("4. Action recommendation (buy, hold, sell).\n\n");
        
        prompt.append("Then, for the overall portfolio:\n");
        prompt.append("- Assess diversification by sector (Layer 1, DeFi, AI, meme, etc.).\n");
        prompt.append("- Identify overexposure or correlation risks.\n");
        prompt.append("- Suggest adjustments to balance risk vs reward.\n\n");
        
        prompt.append("Portfolio data:\n\n");
        
        // Add detailed portfolio information
        for (Holding holding : holdings) {
            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("Expected Target Price: $%.2f\n", holding.getExpectedPrice()));
            prompt.append(String.format("3-Month Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("Long-term Target: $%.2f\n", holding.getTargetPriceLongTerm()));
            prompt.append(String.format("Initial Investment Value: $%.2f\n", holding.getInitialValue()));
            prompt.append("\n");
        }
        
        // Add context and output format
        prompt.append("--- Context ---\n");
        prompt.append("Investment timeframe: 6 months – 3 years\n");
        prompt.append("Risk tolerance: Moderate\n");
        prompt.append("Focus: Strong fundamentals, real-world utility, sustainable long-term value\n");
        prompt.append("Exclusions: No meme coins or highly speculative low-cap tokens\n\n");
        
        prompt.append("--- Output Format ---\n");
        prompt.append("Please provide your analysis in the following structured JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"individual_analysis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"BTC\",\n");
        prompt.append("      \"market_outlook_short\": \"Short-term outlook analysis\",\n");
        prompt.append("      \"market_outlook_long\": \"Long-term outlook analysis\",\n");
        prompt.append("      \"risk_factors\": \"Key risk factors to monitor\",\n");
        prompt.append("      \"upside_opportunities\": \"Potential growth drivers\",\n");
        prompt.append("      \"action_recommendation\": \"BUY|HOLD|SELL\",\n");
        prompt.append("      \"rationale\": \"Reasoning for the recommendation\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"portfolio_analysis\": {\n");
        prompt.append("    \"sector_diversification\": {\n");
        prompt.append("      \"layer1_exposure\": \"Analysis of Layer 1 exposure\",\n");
        prompt.append("      \"layer2_exposure\": \"Analysis of Layer 2 exposure\",\n");
        prompt.append("      \"defi_exposure\": \"Analysis of DeFi exposure\",\n");
        prompt.append("      \"ai_exposure\": \"Analysis of AI/ML exposure\",\n");
        prompt.append("      \"other_sectors\": \"Analysis of other sectors\"\n");
        prompt.append("    },\n");
        prompt.append("    \"correlation_risks\": \"Assessment of correlation and concentration risks\",\n");
        prompt.append("    \"overexposure_concerns\": \"Areas of potential overexposure\",\n");
        prompt.append("    \"diversification_score\": \"POOR|FAIR|GOOD|EXCELLENT\",\n");
        prompt.append("    \"recommended_adjustments\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"action\": \"Description of recommended action\",\n");
        prompt.append("        \"rationale\": \"Why this adjustment is recommended\",\n");
        prompt.append("        \"priority\": \"HIGH|MEDIUM|LOW\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"risk_reward_balance\": \"Overall assessment of risk vs reward balance\",\n");
        prompt.append("    \"missing_sectors\": [\"List of missing sectors that could improve diversification\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"overall_recommendation\": \"AGGRESSIVE_GROWTH|BALANCED_GROWTH|CONSERVATIVE|REBALANCE_NEEDED\",\n");
        prompt.append("  \"summary\": \"Executive summary of the analysis and key recommendations\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private String buildPromptForOverview(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a crypto investment advisor. Analyze the following portfolio and provide structured advice.\n\n");

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
        prompt.append("- Investment timeframe: 6 months – 3 years\n");
        prompt.append("- Risk tolerance: Moderate\n");
        prompt.append("- Focus: Strong fundamentals, real-world utility, and sustainable long-term value\n");
        prompt.append("- Exclusions: No meme coins or highly speculative low-cap tokens\n\n");

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
            prompt.append(String.format("Expected Target Price: $%.2f\n", holding.getExpectedPrice()));
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
        prompt.append("Investment timeframe: 6 months – 3 years\n");
        prompt.append("Risk tolerance: Moderate\n");
        prompt.append("Portfolio size: $" + String.format("%.0f", totalPortfolioValue) + "\n");
        prompt.append("Focus: Strong fundamentals, sustainable growth, risk management\n\n");
        
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

    private Map<String, Object> parseRiskOpportunityResponse(String response) {
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
            
            // Parse individual analysis
            if (responseNode.has("individual_analysis")) {
                List<Map<String, String>> individualAnalysis = new ArrayList<>();
                JsonNode individualArray = responseNode.get("individual_analysis");
                
                for (JsonNode analysis : individualArray) {
                    Map<String, String> coin = new HashMap<>();
                    coin.put("symbol", analysis.has("symbol") ? analysis.get("symbol").asText() : "");
                    coin.put("market_outlook_short", analysis.has("market_outlook_short") ? analysis.get("market_outlook_short").asText() : "");
                    coin.put("market_outlook_long", analysis.has("market_outlook_long") ? analysis.get("market_outlook_long").asText() : "");
                    coin.put("risk_factors", analysis.has("risk_factors") ? analysis.get("risk_factors").asText() : "");
                    coin.put("upside_opportunities", analysis.has("upside_opportunities") ? analysis.get("upside_opportunities").asText() : "");
                    coin.put("action_recommendation", analysis.has("action_recommendation") ? analysis.get("action_recommendation").asText() : "HOLD");
                    coin.put("rationale", analysis.has("rationale") ? analysis.get("rationale").asText() : "");
                    individualAnalysis.add(coin);
                }
                parsedData.put("individual_analysis", individualAnalysis);
            }
            
            // Parse portfolio analysis
            if (responseNode.has("portfolio_analysis")) {
                JsonNode portfolioNode = responseNode.get("portfolio_analysis");
                Map<String, Object> portfolioAnalysis = new HashMap<>();
                
                // Parse sector diversification
                if (portfolioNode.has("sector_diversification")) {
                    JsonNode sectorNode = portfolioNode.get("sector_diversification");
                    Map<String, String> sectorDiv = new HashMap<>();
                    sectorDiv.put("layer1_exposure", sectorNode.has("layer1_exposure") ? sectorNode.get("layer1_exposure").asText() : "");
                    sectorDiv.put("layer2_exposure", sectorNode.has("layer2_exposure") ? sectorNode.get("layer2_exposure").asText() : "");
                    sectorDiv.put("defi_exposure", sectorNode.has("defi_exposure") ? sectorNode.get("defi_exposure").asText() : "");
                    sectorDiv.put("ai_exposure", sectorNode.has("ai_exposure") ? sectorNode.get("ai_exposure").asText() : "");
                    sectorDiv.put("other_sectors", sectorNode.has("other_sectors") ? sectorNode.get("other_sectors").asText() : "");
                    portfolioAnalysis.put("sector_diversification", sectorDiv);
                }
                
                portfolioAnalysis.put("correlation_risks", portfolioNode.has("correlation_risks") ? portfolioNode.get("correlation_risks").asText() : "");
                portfolioAnalysis.put("overexposure_concerns", portfolioNode.has("overexposure_concerns") ? portfolioNode.get("overexposure_concerns").asText() : "");
                portfolioAnalysis.put("diversification_score", portfolioNode.has("diversification_score") ? portfolioNode.get("diversification_score").asText() : "FAIR");
                
                // Parse recommended adjustments
                if (portfolioNode.has("recommended_adjustments")) {
                    List<Map<String, String>> adjustments = new ArrayList<>();
                    JsonNode adjustmentsArray = portfolioNode.get("recommended_adjustments");
                    
                    for (JsonNode adjustment : adjustmentsArray) {
                        Map<String, String> adj = new HashMap<>();
                        adj.put("action", adjustment.has("action") ? adjustment.get("action").asText() : "");
                        adj.put("rationale", adjustment.has("rationale") ? adjustment.get("rationale").asText() : "");
                        adj.put("priority", adjustment.has("priority") ? adjustment.get("priority").asText() : "MEDIUM");
                        adjustments.add(adj);
                    }
                    portfolioAnalysis.put("recommended_adjustments", adjustments);
                }
                
                portfolioAnalysis.put("risk_reward_balance", portfolioNode.has("risk_reward_balance") ? portfolioNode.get("risk_reward_balance").asText() : "");
                
                // Parse missing sectors
                if (portfolioNode.has("missing_sectors")) {
                    List<String> missingSectors = new ArrayList<>();
                    JsonNode missingSectorsArray = portfolioNode.get("missing_sectors");
                    for (JsonNode sector : missingSectorsArray) {
                        missingSectors.add(sector.asText());
                    }
                    portfolioAnalysis.put("missing_sectors", missingSectors);
                }
                
                parsedData.put("portfolio_analysis", portfolioAnalysis);
            }
            
            // Parse top-level fields
            parsedData.put("overall_recommendation", responseNode.has("overall_recommendation") ? responseNode.get("overall_recommendation").asText() : "BALANCED_GROWTH");
            parsedData.put("summary", responseNode.has("summary") ? responseNode.get("summary").asText() : "Risk and opportunity analysis completed");
            
        } catch (Exception e) {
            System.err.println("Error parsing risk opportunity analysis response: " + e.getMessage());
            e.printStackTrace();
            
            // Set default values if parsing fails
            parsedData.put("overall_recommendation", "BALANCED_GROWTH");
            parsedData.put("summary", "Unable to parse risk analysis due to API response parsing error");
            parsedData.put("individual_analysis", new ArrayList<>());
            parsedData.put("portfolio_analysis", new HashMap<>());
        }
        
        return parsedData;
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
}
