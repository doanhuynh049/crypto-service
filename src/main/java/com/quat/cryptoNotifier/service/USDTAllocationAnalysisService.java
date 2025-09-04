package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.MarketData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class USDTAllocationAnalysisService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private DataProviderService dataProviderService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Builds a comprehensive USDT allocation prompt for AI analysis
     * @param holdings Current portfolio holdings
     * @param usdtAmount Available USDT amount for deployment
     * @return Formatted prompt string
     */
    public String buildUSDTAllocationPrompt(List<Holding> holdings, double usdtAmount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("USDT Allocation Strategy Analysis Prompt\n");
        prompt.append("This analysis is based on my overall investment strategy and current portfolio holdings.\n\n");
        prompt.append(investmentStrategyService.getCompleteInvestmentStrategySection());
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

    /**
     * Parses USDT allocation AI response into structured data
     * @param response Raw AI response string
     * @return Parsed data as Map
     */
    public Map<String, Object> parseUSDTAllocationResponse(String response) {
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
