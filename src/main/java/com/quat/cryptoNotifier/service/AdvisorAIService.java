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

/**
 * AdvisorAI Service for comprehensive portfolio analysis and strategic recommendations.
 * This service analyzes current investment strategy and target allocation of holdings,
 * providing AI-driven recommendations for optimal portfolio management.
 */
@Service
public class AdvisorAIService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private DataProviderService dataProviderService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Builds a comprehensive investment strategy and target review prompt for AI
     * @param holdings Current portfolio holdings
     * @return Formatted prompt string
     */
    public String buildInvestmentStrategyPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Investment Strategy & Target Price Review Analysis\n");
        prompt.append("===============================================\n\n");
        prompt.append("I need you to review my current investment strategy and the target prices I've set for my holdings.\n");
        prompt.append("Analyze whether my strategy is still optimal and if my price targets need updating based on current market conditions and fundamentals.\n\n");
        
        // Add investment strategy context
        prompt.append("My Current Investment Strategy:\n");
        prompt.append(investmentStrategyService.getCompleteInvestmentStrategySection());
        
        prompt.append("Current Portfolio Holdings & Targets Review:\n");
        prompt.append("==========================================\n\n");

        // Calculate total portfolio value and current allocations
        double totalPortfolioValue = 0;
        Map<String, Double> sectorAllocations = new HashMap<>();
        
        for (Holding holding : holdings) {
            double value = holding.getTotalAvgCost();
            totalPortfolioValue += value;
            
            String sector = holding.getSector() != null ? holding.getSector() : "Unknown";
            sectorAllocations.put(sector, sectorAllocations.getOrDefault(sector, 0.0) + value);
        }

        // Add detailed holdings analysis with target review focus
        for (Holding holding : holdings) {
            if (holding.getSymbol().equals("USDT")) continue; // Skip stablecoin analysis
            
            double currentWeight = (holding.getTotalAvgCost() / totalPortfolioValue) * 100;
            
            // Get current price from market data
            double currentPrice = holding.getAveragePrice(); // fallback to avg price
            try {
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    currentPrice = marketData.getCurrentPrice();
                }
            } catch (Exception e) {
                // Use average price as fallback
            }
            
            double currentValue = holding.getHoldings() * currentPrice;
            double profitLoss = currentValue - holding.getTotalAvgCost();
            double profitLossPercentage = (profitLoss / holding.getTotalAvgCost()) * 100;

            prompt.append(String.format("=== %s (%s) - %s Sector ===\n", holding.getSymbol(), holding.getName(), holding.getSector()));
            prompt.append(String.format("Current Position:\n"));
            prompt.append(String.format("  • Holdings: %.6f %s (%.1f%% of portfolio)\n", holding.getHoldings(), holding.getSymbol(), currentWeight));
            prompt.append(String.format("  • Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("  • Current Price: $%.2f\n", currentPrice));
            prompt.append(String.format("  • Current P&L: $%.2f (%.1f%%)\n", profitLoss, profitLossPercentage));
            
            prompt.append(String.format("\nMY CURRENT TARGETS TO REVIEW:\n"));
            prompt.append(String.format("  • Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("  • 3-Month Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("  • Long-term Target: $%.2f\n", holding.getTargetPriceLongTerm()));
            
            // Calculate realistic returns from current price
            double potential3MonthReturn = ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100;
            double potentialLongTermReturn = ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100;
            prompt.append(String.format("  • Potential 3M Return from Current Price: %.1f%%\n", potential3MonthReturn));
            prompt.append(String.format("  • Potential Long-term Return from Current Price: %.1f%%\n", potentialLongTermReturn));
            prompt.append("\n");
        }

        // Add current sector allocation vs strategy
        prompt.append("Current vs Target Sector Allocation:\n");
        prompt.append("===================================\n");
        for (Map.Entry<String, Double> entry : sectorAllocations.entrySet()) {
            double percentage = (entry.getValue() / totalPortfolioValue) * 100;
            prompt.append(String.format("- %s: %.1f%% (Currently $%.2f)\n", entry.getKey(), percentage, entry.getValue()));
        }
        prompt.append("\n");

        // Add market context
        prompt.append("Current Market Context:\n");
        prompt.append("======================\n");
        try {
            for (Holding holding : holdings) {
                if (holding.getSymbol().equals("USDT")) continue;
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    double currentPrice = marketData.getCurrentPrice();
                    double priceChange24h = marketData.getPriceChangePercentage24h();
                    prompt.append(String.format("%s: $%.2f (%.2f%% 24h change)\n", holding.getSymbol(), currentPrice, priceChange24h));
                }
            }
        } catch (Exception e) {
            prompt.append("Market data temporarily unavailable for analysis\n");
        }
        prompt.append("\n");

        prompt.append("SPECIFIC ANALYSIS REQUIRED:\n");
        prompt.append("==========================\n");
        prompt.append("1. STRATEGY REVIEW: Is my current investment strategy still optimal for current market conditions?\n");
        prompt.append("2. TARGET PRICE VALIDATION: Are my 3-month and long-term targets realistic based on:\n");
        prompt.append("   - Current fundamentals and technical analysis\n");
        prompt.append("   - Market cycles and broader crypto trends\n");
        prompt.append("   - Sector performance and competition\n");
        prompt.append("   - Project development and adoption metrics\n");
        prompt.append("3. TARGET UPDATES NEEDED: Which targets should I update (increase/decrease) and why?\n");
        prompt.append("4. EXPECTED ENTRY PRICES: Are my expected entry prices still valid or should I adjust them?\n");
        prompt.append("5. PORTFOLIO ALLOCATION: Should I adjust my allocation strategy given current market conditions?\n");
        prompt.append("6. RISK ASSESSMENT: Are there any holdings that no longer fit my strategy?\n");
        prompt.append("7. NEW OPPORTUNITIES: Any sectors/assets I should consider adding to align with my strategy?\n\n");

        prompt.append("--- Output Format ---\n");
        prompt.append("Please structure your analysis in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"strategy_review\": {\n");
        prompt.append("    \"strategy_validity_score\": \"1-10 score\",\n");
        prompt.append("    \"current_strategy_assessment\": \"EXCELLENT|GOOD|NEEDS_MINOR_UPDATES|NEEDS_MAJOR_UPDATES|OUTDATED\",\n");
        prompt.append("    \"strategy_strengths\": [\"Current strategy strengths\"],\n");
        prompt.append("    \"strategy_weaknesses\": [\"Areas that need improvement\"],\n");
        prompt.append("    \"recommended_strategy_updates\": [\"Specific strategy changes needed\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"target_price_review\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"ETH\",\n");
        prompt.append("      \"current_targets_assessment\": \"REALISTIC|OPTIMISTIC|CONSERVATIVE|UNREALISTIC\",\n");
        prompt.append("      \"current_3month_target\": \"$5000\",\n");
        prompt.append("      \"recommended_3month_target\": \"$4500\",\n");
        prompt.append("      \"3month_target_change\": \"INCREASE|DECREASE|MAINTAIN\",\n");
        prompt.append("      \"current_longterm_target\": \"$10000\",\n");
        prompt.append("      \"recommended_longterm_target\": \"$8500\",\n");
        prompt.append("      \"longterm_target_change\": \"INCREASE|DECREASE|MAINTAIN\",\n");
        prompt.append("      \"target_rationale\": \"Why these targets are recommended\",\n");
        prompt.append("      \"risk_factors\": [\"Factors that could affect target achievement\"],\n");
        prompt.append("      \"catalysts\": [\"Events/factors that support target achievement\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"expected_entry_review\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"ETH\",\n");
        prompt.append("      \"current_expected_entry\": \"$4200\",\n");
        prompt.append("      \"recommended_expected_entry\": \"$3800\",\n");
        prompt.append("      \"entry_change\": \"INCREASE|DECREASE|MAINTAIN\",\n");
        prompt.append("      \"entry_rationale\": \"Why this entry price is recommended\",\n");
        prompt.append("      \"market_timing\": \"Current market conditions for entry\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"portfolio_allocation_review\": {\n");
        prompt.append("    \"current_allocation_assessment\": \"OPTIMAL|GOOD|NEEDS_REBALANCING|POOR\",\n");
        prompt.append("    \"sector_allocation_changes\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"sector\": \"Layer 1\",\n");
        prompt.append("        \"current_percentage\": \"60%\",\n");
        prompt.append("        \"recommended_percentage\": \"50%\",\n");
        prompt.append("        \"change_needed\": \"INCREASE|DECREASE|MAINTAIN\",\n");
        prompt.append("        \"rationale\": \"Why this change is recommended\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"individual_allocation_changes\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"ETH\",\n");
        prompt.append("        \"current_weight\": \"25%\",\n");
        prompt.append("        \"recommended_weight\": \"30%\",\n");
        prompt.append("        \"action\": \"INCREASE|DECREASE|MAINTAIN|REMOVE\",\n");
        prompt.append("        \"rationale\": \"Why this allocation change is recommended\"\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"holdings_review\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"ETH\",\n");
        prompt.append("      \"strategy_fit_score\": \"1-10\",\n");
        prompt.append("      \"still_fits_strategy\": \"true|false\",\n");
        prompt.append("      \"performance_assessment\": \"EXCELLENT|GOOD|FAIR|POOR|UNDERPERFORMING\",\n");
        prompt.append("      \"recommendation\": \"INCREASE|MAINTAIN|REDUCE|SELL\",\n");
        prompt.append("      \"rationale\": \"Why this recommendation\",\n");
        prompt.append("      \"concerns\": [\"Any concerns about this holding\"],\n");
        prompt.append("      \"opportunities\": [\"Opportunities for this holding\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"new_opportunities\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"sector\": \"Real World Assets\",\n");
        prompt.append("      \"recommended_assets\": [\"ONDO\", \"RWA\"],\n");
        prompt.append("      \"allocation_suggestion\": \"5-8%\",\n");
        prompt.append("      \"rationale\": \"Why this sector/asset should be added\",\n");
        prompt.append("      \"fits_strategy\": \"true|false\",\n");
        prompt.append("      \"timeline\": \"Immediate|Short-term|Long-term\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"market_timing_assessment\": {\n");
        prompt.append("    \"current_market_phase\": \"BULL|BEAR|SIDEWAYS|UNCERTAIN\",\n");
        prompt.append("    \"timing_for_updates\": \"IMMEDIATE|GRADUAL|WAIT_FOR_DIP|WAIT_FOR_RALLY\",\n");
        prompt.append("    \"market_risks\": [\"Current market risks to consider\"],\n");
        prompt.append("    \"market_opportunities\": [\"Current market opportunities\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"action_plan\": {\n");
        prompt.append("    \"immediate_actions\": [\"Actions to take this week\"],\n");
        prompt.append("    \"target_updates_priority\": [\"Which targets to update first\"],\n");
        prompt.append("    \"portfolio_adjustments\": [\"Portfolio changes to make\"],\n");
        prompt.append("    \"monitoring_list\": [\"Assets/metrics to monitor closely\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"summary_recommendations\": [\n");
        prompt.append("    \"Top 5 most important recommendations with specific targets and rationale\"\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("Ensure your analysis is:\n");
        prompt.append("- Comprehensive and specific with exact percentages and dollar amounts where applicable\n");
        prompt.append("- Aligned with my moderate risk tolerance and 6-month to 3-year timeframe\n");
        prompt.append("- Focused on projects with strong fundamentals and real-world utility\n");
        prompt.append("- Considerate of current market conditions and timing\n");
        prompt.append("- Actionable with clear priorities and timelines\n");
        prompt.append("- Data-driven based on current portfolio performance and targets\n");

        return prompt.toString();
    }

    /**
     * Parses Investment Strategy AI response into structured data
     * @param response Raw AI response string
     * @return Parsed data as Map
     */
    public Map<String, Object> parseInvestmentStrategyResponse(String response) {
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

            // Parse strategy review
            if (responseNode.has("strategy_review")) {
                JsonNode strategyNode = responseNode.get("strategy_review");
                Map<String, Object> strategyReview = new HashMap<>();
                
                strategyReview.put("strategy_validity_score", strategyNode.has("strategy_validity_score") ? strategyNode.get("strategy_validity_score").asText() : "N/A");
                strategyReview.put("current_strategy_assessment", strategyNode.has("current_strategy_assessment") ? strategyNode.get("current_strategy_assessment").asText() : "GOOD");
                
                // Parse strategy strengths
                if (strategyNode.has("strategy_strengths")) {
                    List<String> strengths = new ArrayList<>();
                    JsonNode strengthsArray = strategyNode.get("strategy_strengths");
                    for (JsonNode strength : strengthsArray) {
                        strengths.add(strength.asText());
                    }
                    strategyReview.put("strategy_strengths", strengths);
                } else {
                    strategyReview.put("strategy_strengths", new ArrayList<>());
                }
                
                // Parse strategy weaknesses
                if (strategyNode.has("strategy_weaknesses")) {
                    List<String> weaknesses = new ArrayList<>();
                    JsonNode weaknessesArray = strategyNode.get("strategy_weaknesses");
                    for (JsonNode weakness : weaknessesArray) {
                        weaknesses.add(weakness.asText());
                    }
                    strategyReview.put("strategy_weaknesses", weaknesses);
                } else {
                    strategyReview.put("strategy_weaknesses", new ArrayList<>());
                }
                
                // Parse recommended strategy updates
                if (strategyNode.has("recommended_strategy_updates")) {
                    List<String> updates = new ArrayList<>();
                    JsonNode updatesArray = strategyNode.get("recommended_strategy_updates");
                    for (JsonNode update : updatesArray) {
                        updates.add(update.asText());
                    }
                    strategyReview.put("recommended_strategy_updates", updates);
                } else {
                    strategyReview.put("recommended_strategy_updates", new ArrayList<>());
                }
                
                parsedData.put("strategy_review", strategyReview);
            }

            // Parse target price review
            if (responseNode.has("target_price_review")) {
                List<Map<String, Object>> targetReviews = new ArrayList<>();
                JsonNode targetsArray = responseNode.get("target_price_review");
                
                for (JsonNode target : targetsArray) {
                    Map<String, Object> targetData = new HashMap<>();
                    targetData.put("symbol", target.has("symbol") ? target.get("symbol").asText() : "");
                    targetData.put("current_targets_assessment", target.has("current_targets_assessment") ? target.get("current_targets_assessment").asText() : "REALISTIC");
                    targetData.put("current_3month_target", target.has("current_3month_target") ? target.get("current_3month_target").asText() : "");
                    targetData.put("recommended_3month_target", target.has("recommended_3month_target") ? target.get("recommended_3month_target").asText() : "");
                    targetData.put("threeMonthTargetChange", target.has("3month_target_change") ? target.get("3month_target_change").asText() : "MAINTAIN");
                    targetData.put("current_longterm_target", target.has("current_longterm_target") ? target.get("current_longterm_target").asText() : "");
                    targetData.put("recommended_longterm_target", target.has("recommended_longterm_target") ? target.get("recommended_longterm_target").asText() : "");
                    targetData.put("longtermTargetChange", target.has("longterm_target_change") ? target.get("longterm_target_change").asText() : "MAINTAIN");
                    targetData.put("target_rationale", target.has("target_rationale") ? target.get("target_rationale").asText() : "Analysis pending");
                    
                    // Parse risk factors
                    if (target.has("risk_factors")) {
                        List<String> risks = new ArrayList<>();
                        JsonNode risksArray = target.get("risk_factors");
                        for (JsonNode risk : risksArray) {
                            risks.add(risk.asText());
                        }
                        targetData.put("risk_factors", risks);
                    } else {
                        targetData.put("risk_factors", new ArrayList<>());
                    }
                    
                    // Parse catalysts
                    if (target.has("catalysts")) {
                        List<String> catalysts = new ArrayList<>();
                        JsonNode catalystsArray = target.get("catalysts");
                        for (JsonNode catalyst : catalystsArray) {
                            catalysts.add(catalyst.asText());
                        }
                        targetData.put("catalysts", catalysts);
                    } else {
                        targetData.put("catalysts", new ArrayList<>());
                    }
                    
                    targetReviews.add(targetData);
                }
                parsedData.put("target_price_review", targetReviews);
            }

            // Parse expected entry review
            if (responseNode.has("expected_entry_review")) {
                List<Map<String, String>> entryReviews = new ArrayList<>();
                JsonNode entriesArray = responseNode.get("expected_entry_review");
                
                for (JsonNode entry : entriesArray) {
                    Map<String, String> entryData = new HashMap<>();
                    entryData.put("symbol", entry.has("symbol") ? entry.get("symbol").asText() : "");
                    entryData.put("current_expected_entry", entry.has("current_expected_entry") ? entry.get("current_expected_entry").asText() : "");
                    entryData.put("recommended_expected_entry", entry.has("recommended_expected_entry") ? entry.get("recommended_expected_entry").asText() : "");
                    entryData.put("entry_change", entry.has("entry_change") ? entry.get("entry_change").asText() : "MAINTAIN");
                    entryData.put("entry_rationale", entry.has("entry_rationale") ? entry.get("entry_rationale").asText() : "Analysis pending");
                    entryData.put("market_timing", entry.has("market_timing") ? entry.get("market_timing").asText() : "Monitor market conditions");
                    entryReviews.add(entryData);
                }
                parsedData.put("expected_entry_review", entryReviews);
            }

            // Parse portfolio allocation review
            if (responseNode.has("portfolio_allocation_review")) {
                JsonNode allocationNode = responseNode.get("portfolio_allocation_review");
                Map<String, Object> allocationReview = new HashMap<>();
                
                allocationReview.put("current_allocation_assessment", allocationNode.has("current_allocation_assessment") ? allocationNode.get("current_allocation_assessment").asText() : "GOOD");
                
                // Parse sector allocation changes
                if (allocationNode.has("sector_allocation_changes")) {
                    List<Map<String, String>> sectorChanges = new ArrayList<>();
                    JsonNode sectorsArray = allocationNode.get("sector_allocation_changes");
                    
                    for (JsonNode sector : sectorsArray) {
                        Map<String, String> sectorData = new HashMap<>();
                        sectorData.put("sector", sector.has("sector") ? sector.get("sector").asText() : "");
                        sectorData.put("current_percentage", sector.has("current_percentage") ? sector.get("current_percentage").asText() : "0%");
                        sectorData.put("recommended_percentage", sector.has("recommended_percentage") ? sector.get("recommended_percentage").asText() : "0%");
                        sectorData.put("change_needed", sector.has("change_needed") ? sector.get("change_needed").asText() : "MAINTAIN");
                        sectorData.put("rationale", sector.has("rationale") ? sector.get("rationale").asText() : "Analysis pending");
                        sectorChanges.add(sectorData);
                    }
                    allocationReview.put("sector_allocation_changes", sectorChanges);
                } else {
                    allocationReview.put("sector_allocation_changes", new ArrayList<>());
                }
                
                // Parse individual allocation changes
                if (allocationNode.has("individual_allocation_changes")) {
                    List<Map<String, String>> individualChanges = new ArrayList<>();
                    JsonNode individualsArray = allocationNode.get("individual_allocation_changes");
                    
                    for (JsonNode individual : individualsArray) {
                        Map<String, String> individualData = new HashMap<>();
                        individualData.put("symbol", individual.has("symbol") ? individual.get("symbol").asText() : "");
                        individualData.put("current_weight", individual.has("current_weight") ? individual.get("current_weight").asText() : "0%");
                        individualData.put("recommended_weight", individual.has("recommended_weight") ? individual.get("recommended_weight").asText() : "0%");
                        individualData.put("action", individual.has("action") ? individual.get("action").asText() : "MAINTAIN");
                        individualData.put("rationale", individual.has("rationale") ? individual.get("rationale").asText() : "Analysis pending");
                        individualChanges.add(individualData);
                    }
                    allocationReview.put("individual_allocation_changes", individualChanges);
                } else {
                    allocationReview.put("individual_allocation_changes", new ArrayList<>());
                }
                
                parsedData.put("portfolio_allocation_review", allocationReview);
            }

            // Parse holdings review
            if (responseNode.has("holdings_review")) {
                List<Map<String, Object>> holdingsReviews = new ArrayList<>();
                JsonNode holdingsArray = responseNode.get("holdings_review");
                
                for (JsonNode holding : holdingsArray) {
                    Map<String, Object> holdingData = new HashMap<>();
                    holdingData.put("symbol", holding.has("symbol") ? holding.get("symbol").asText() : "");
                    holdingData.put("strategy_fit_score", holding.has("strategy_fit_score") ? holding.get("strategy_fit_score").asText() : "5");
                    holdingData.put("still_fits_strategy", holding.has("still_fits_strategy") ? holding.get("still_fits_strategy").asText() : "true");
                    holdingData.put("performance_assessment", holding.has("performance_assessment") ? holding.get("performance_assessment").asText() : "FAIR");
                    holdingData.put("recommendation", holding.has("recommendation") ? holding.get("recommendation").asText() : "MAINTAIN");
                    holdingData.put("rationale", holding.has("rationale") ? holding.get("rationale").asText() : "Analysis pending");
                    
                    // Parse concerns
                    if (holding.has("concerns")) {
                        List<String> concerns = new ArrayList<>();
                        JsonNode concernsArray = holding.get("concerns");
                        for (JsonNode concern : concernsArray) {
                            concerns.add(concern.asText());
                        }
                        holdingData.put("concerns", concerns);
                    } else {
                        holdingData.put("concerns", new ArrayList<>());
                    }
                    
                    // Parse opportunities
                    if (holding.has("opportunities")) {
                        List<String> opportunities = new ArrayList<>();
                        JsonNode opportunitiesArray = holding.get("opportunities");
                        for (JsonNode opportunity : opportunitiesArray) {
                            opportunities.add(opportunity.asText());
                        }
                        holdingData.put("opportunities", opportunities);
                    } else {
                        holdingData.put("opportunities", new ArrayList<>());
                    }
                    
                    holdingsReviews.add(holdingData);
                }
                parsedData.put("holdings_review", holdingsReviews);
            }

            // Parse new opportunities
            if (responseNode.has("new_opportunities")) {
                List<Map<String, Object>> newOpportunities = new ArrayList<>();
                JsonNode opportunitiesArray = responseNode.get("new_opportunities");
                
                for (JsonNode opportunity : opportunitiesArray) {
                    Map<String, Object> opportunityData = new HashMap<>();
                    opportunityData.put("sector", opportunity.has("sector") ? opportunity.get("sector").asText() : "");
                    
                    // Parse recommended assets
                    if (opportunity.has("recommended_assets")) {
                        List<String> assets = new ArrayList<>();
                        JsonNode assetsArray = opportunity.get("recommended_assets");
                        for (JsonNode asset : assetsArray) {
                            assets.add(asset.asText());
                        }
                        opportunityData.put("recommended_assets", assets);
                    } else {
                        opportunityData.put("recommended_assets", new ArrayList<>());
                    }
                    
                    opportunityData.put("allocation_suggestion", opportunity.has("allocation_suggestion") ? opportunity.get("allocation_suggestion").asText() : "0%");
                    opportunityData.put("rationale", opportunity.has("rationale") ? opportunity.get("rationale").asText() : "Analysis pending");
                    opportunityData.put("fits_strategy", opportunity.has("fits_strategy") ? opportunity.get("fits_strategy").asText() : "true");
                    opportunityData.put("timeline", opportunity.has("timeline") ? opportunity.get("timeline").asText() : "Short-term");
                    
                    newOpportunities.add(opportunityData);
                }
                parsedData.put("new_opportunities", newOpportunities);
            }

            // Parse market timing assessment
            if (responseNode.has("market_timing_assessment")) {
                JsonNode timingNode = responseNode.get("market_timing_assessment");
                Map<String, Object> timingAssessment = new HashMap<>();
                
                timingAssessment.put("current_market_phase", timingNode.has("current_market_phase") ? timingNode.get("current_market_phase").asText() : "UNCERTAIN");
                timingAssessment.put("timing_for_updates", timingNode.has("timing_for_updates") ? timingNode.get("timing_for_updates").asText() : "GRADUAL");
                
                // Parse market risks
                if (timingNode.has("market_risks")) {
                    List<String> risks = new ArrayList<>();
                    JsonNode risksArray = timingNode.get("market_risks");
                    for (JsonNode risk : risksArray) {
                        risks.add(risk.asText());
                    }
                    timingAssessment.put("market_risks", risks);
                } else {
                    timingAssessment.put("market_risks", new ArrayList<>());
                }
                
                // Parse market opportunities
                if (timingNode.has("market_opportunities")) {
                    List<String> opportunities = new ArrayList<>();
                    JsonNode opportunitiesArray = timingNode.get("market_opportunities");
                    for (JsonNode opportunity : opportunitiesArray) {
                        opportunities.add(opportunity.asText());
                    }
                    timingAssessment.put("market_opportunities", opportunities);
                } else {
                    timingAssessment.put("market_opportunities", new ArrayList<>());
                }
                
                parsedData.put("market_timing_assessment", timingAssessment);
            }

            // Parse action plan
            if (responseNode.has("action_plan")) {
                JsonNode actionNode = responseNode.get("action_plan");
                Map<String, Object> actionPlan = new HashMap<>();
                
                // Parse immediate actions
                if (actionNode.has("immediate_actions")) {
                    List<String> immediateActions = new ArrayList<>();
                    JsonNode immediateArray = actionNode.get("immediate_actions");
                    for (JsonNode action : immediateArray) {
                        immediateActions.add(action.asText());
                    }
                    actionPlan.put("immediate_actions", immediateActions);
                } else {
                    actionPlan.put("immediate_actions", new ArrayList<>());
                }
                
                // Parse target updates priority
                if (actionNode.has("target_updates_priority")) {
                    List<String> targetUpdates = new ArrayList<>();
                    JsonNode targetsArray = actionNode.get("target_updates_priority");
                    for (JsonNode target : targetsArray) {
                        targetUpdates.add(target.asText());
                    }
                    actionPlan.put("target_updates_priority", targetUpdates);
                } else {
                    actionPlan.put("target_updates_priority", new ArrayList<>());
                }
                
                // Parse portfolio adjustments
                if (actionNode.has("portfolio_adjustments")) {
                    List<String> adjustments = new ArrayList<>();
                    JsonNode adjustmentsArray = actionNode.get("portfolio_adjustments");
                    for (JsonNode adjustment : adjustmentsArray) {
                        adjustments.add(adjustment.asText());
                    }
                    actionPlan.put("portfolio_adjustments", adjustments);
                } else {
                    actionPlan.put("portfolio_adjustments", new ArrayList<>());
                }
                
                // Parse monitoring list
                if (actionNode.has("monitoring_list")) {
                    List<String> monitoring = new ArrayList<>();
                    JsonNode monitoringArray = actionNode.get("monitoring_list");
                    for (JsonNode item : monitoringArray) {
                        monitoring.add(item.asText());
                    }
                    actionPlan.put("monitoring_list", monitoring);
                } else {
                    actionPlan.put("monitoring_list", new ArrayList<>());
                }
                
                parsedData.put("action_plan", actionPlan);
            }

            // Parse summary recommendations
            if (responseNode.has("summary_recommendations")) {
                List<String> summaryRecommendations = new ArrayList<>();
                JsonNode recommendationsArray = responseNode.get("summary_recommendations");
                for (JsonNode recommendation : recommendationsArray) {
                    summaryRecommendations.add(recommendation.asText());
                }
                parsedData.put("summary_recommendations", summaryRecommendations);
            } else {
                parsedData.put("summary_recommendations", new ArrayList<>());
            }

        } catch (Exception e) {
            System.err.println("Error parsing Investment Strategy response: " + e.getMessage());
            e.printStackTrace();

            // Set default values if parsing fails
            parsedData.put("strategy_review", createDefaultStrategyReview());
            parsedData.put("target_price_review", new ArrayList<>());
            parsedData.put("expected_entry_review", new ArrayList<>());
            parsedData.put("portfolio_allocation_review", new HashMap<>());
            parsedData.put("holdings_review", new ArrayList<>());
            parsedData.put("new_opportunities", new ArrayList<>());
            parsedData.put("market_timing_assessment", new HashMap<>());
            parsedData.put("action_plan", new HashMap<>());
            parsedData.put("summary_recommendations", new ArrayList<>());
        }

        return parsedData;
    }

    /**
     * Creates default strategy review values in case of parsing errors
     */
    private Map<String, Object> createDefaultStrategyReview() {
        Map<String, Object> defaultReview = new HashMap<>();
        defaultReview.put("strategy_validity_score", "N/A");
        defaultReview.put("current_strategy_assessment", "GOOD");
        defaultReview.put("strategy_strengths", new ArrayList<>());
        defaultReview.put("strategy_weaknesses", new ArrayList<>());
        defaultReview.put("recommended_strategy_updates", new ArrayList<>());
        return defaultReview;
    }
}
