package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.Holding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioHealthCheckAnalysisService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    private final ObjectMapper objectMapper;

    public PortfolioHealthCheckAnalysisService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Builds a comprehensive portfolio health check prompt for AI analysis
     * @param holdings List of portfolio holdings
     * @return Formatted prompt string for portfolio health analysis
     */
    public String buildPortfolioHealthCheckPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Portfolio Health Check Prompt\n");
        prompt.append("This analysis is based on my overall investment strategy and current portfolio holdings.\n\n");
        prompt.append(investmentStrategyService.getCompleteInvestmentStrategySection());
        prompt.append("Check the health of my crypto portfolio. Based on the data, tell me:\n");
        prompt.append("- Which coins are over-weighted or under-weighted.\n");
        prompt.append("- Which target prices look unrealistic.\n");
        prompt.append("- Where I should consider taking profit.\n");
        prompt.append("- How much stablecoin buffer I should keep.\n\n");
        
        prompt.append("Portfolio data:\n\n");
        
        // Calculate total portfolio value and individual percentages
        double totalPortfolioValue = 0;
        for (Holding holding : holdings) {
            totalPortfolioValue += holding.getTotalAvgCost();
        }
        
        // Add detailed portfolio information with weightings
        for (Holding holding : holdings) {
            double currentWeight = (holding.getTotalAvgCost() / totalPortfolioValue) * 100;
            
            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Sector: %s\n", holding.getSector() != null ? holding.getSector() : "Unknown"));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Initial Investment: $%.2f (%.1f%% of portfolio)\n", holding.getTotalAvgCost(), currentWeight));
            prompt.append(String.format("Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("Expected Deep Entry Price: $%.2f\n", holding.getDeepEntryPrice()));
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

    /**
     * Parses the AI response for portfolio health check analysis
     * @param response Raw AI response string
     * @return Structured data map with parsed portfolio health analysis
     */
    public Map<String, Object> parsePortfolioHealthCheckResponse(String response) {
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
