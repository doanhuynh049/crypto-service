package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParseException;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.MarketData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service dedicated to portfolio optimization analysis functionality.
 * 
 * PURPOSE:
 * This service is responsible for providing comprehensive portfolio optimization recommendations by:
 * 1. Analyzing overall risk profile of the portfolio considering asset class exposure
 * 2. Evaluating balance between upside potential and downside risk
 * 3. Identifying overweighted or underweighted assets based on fundamentals
 * 4. Providing actionable rebalancing suggestions for better diversification
 * 5. Recommending missing sectors or assets for portfolio enhancement
 * 
 * The analysis helps users optimize their portfolio allocation for both growth potential
 * and risk management, considering sector concentration, correlation risks, and
 * strategic opportunities for better long-term performance.
 */
@Service
public class PortfolioOptimizationAnalysisService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private DataProviderService dataProviderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build a comprehensive portfolio optimization analysis prompt.
     * 
     * This method creates a detailed prompt that asks the AI to analyze the portfolio's
     * risk profile, balance between upside and downside potential, weight distribution,
     * and provide actionable rebalancing recommendations along with missing assets analysis.
     */
     public String buildPortfolioOptimizationPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("🔍 Portfolio Optimization Analysis Prompt\n");
        prompt.append("This analysis is based on my overall investment strategy and current portfolio holdings.\n\n");
        prompt.append(investmentStrategyService.getCompleteInvestmentStrategySection());
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
            totalPortfolioValue += holding.getTotalAvgCost();
        }
        
        // Add detailed portfolio information with current weightings
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

    public Map<String, Object> parsePortfolioOptimizationResponse(String response) {
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
}
