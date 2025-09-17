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
 * Service dedicated to risk and opportunity analysis functionality.
 * Handles prompt building and response parsing for portfolio risk assessment.
 */
@Service
public class RiskOpportunityAnalysisService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private DataProviderService dataProviderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build a comprehensive risk and opportunity analysis prompt with current market data
     */
    public String buildRiskOpportunityPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Risk & Opportunity Analysis Prompt\n");
        prompt.append("This analysis is based on my overall investment strategy and current portfolio holdings.\n\n");
        prompt.append(investmentStrategyService.getCompleteInvestmentStrategySection());
        prompt.append("Analyze my crypto portfolio below. For each coin, provide:\n");
        prompt.append("1. Current market outlook (short-term vs long-term).\n");
        prompt.append("2. Risk factors (volatility, liquidity, fundamentals).\n");
        prompt.append("3. Upside opportunities.\n");
        prompt.append("4. Action recommendation (buy, hold, sell).\n\n");
        
        prompt.append("Then, for the overall portfolio:\n");
        prompt.append("- Assess diversification by sector (Layer 1, DeFi, AI, meme, etc.).\n");
        prompt.append("- Identify overexposure or correlation risks.\n");
        prompt.append("- Suggest adjustments to balance risk vs reward.\n\n");
        
        prompt.append("Portfolio data with current market information:\n\n");

        // Add detailed portfolio information with current market data
        double totalPortfolioValue = 0;
        double totalInvestmentCost = 0;

        for (Holding holding : holdings) {
            if ("USDT".equals(holding.getSymbol()) || "USDC".equals(holding.getSymbol()) || "BUSD".equals(holding.getSymbol())) {
                continue; // Skip stablecoins for market analysis
            }

            // Get current market data
            MarketData marketData = null;
            double currentPrice = 0;
            double currentValue = 0;
            double profitLoss = 0;
            double profitLossPercentage = 0;
            String technicalAnalysis = "N/A";

            try {
                marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    currentPrice = marketData.getCurrentPrice();
                    currentValue = holding.getHoldings() * currentPrice;
                    profitLoss = currentValue - holding.getTotalAvgCost();
                    profitLossPercentage = ((currentPrice - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
                    technicalAnalysis = buildTechnicalAnalysis(marketData);
                }
            } catch (Exception e) {
                System.err.println("Error fetching market data for " + holding.getSymbol() + ": " + e.getMessage());
                currentPrice = 0; // Fallback to average price
                currentValue = 0;
            }

            totalPortfolioValue += currentValue;
            totalInvestmentCost += holding.getTotalAvgCost();

            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Sector: %s\n", holding.getSector() != null ? holding.getSector() : "Unknown"));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Current Price: $%.2f\n", currentPrice != 0 ? currentPrice : "Unknown"));
            prompt.append(String.format("Current Value: $%.2f\n", currentValue != 0 ? currentValue : "Unknown"));
            prompt.append(String.format("Initial Investment: $%.2f\n", holding.getTotalAvgCost()));
            prompt.append(String.format("Profit/Loss: $%.2f (%.2f%%)\n", profitLoss, profitLossPercentage));
            prompt.append(String.format("Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("Expected Deep Entry Price: $%.2f\n", holding.getDeepEntryPrice()));
            prompt.append(String.format("3-Month Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("Long-term Target: $%.2f\n", holding.getTargetPriceLongTerm()));

            // Add technical analysis if available
            if (!"N/A".equals(technicalAnalysis)) {
                prompt.append(String.format("Technical Analysis: %s\n", technicalAnalysis));
            }

            // Add price action context
            double percentToTarget3M = ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100;
            double percentToTargetLT = ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100;
            prompt.append(String.format("Distance to 3M Target: %.1f%%\n", percentToTarget3M));
            prompt.append(String.format("Distance to LT Target: %.1f%%\n", percentToTargetLT));
            prompt.append("\n");
        }
        
        // Add portfolio summary
        double totalProfitLoss = totalPortfolioValue - totalInvestmentCost;
        double totalProfitLossPercentage = totalInvestmentCost > 0 ? (totalProfitLoss / totalInvestmentCost) * 100 : 0;

        prompt.append("--- Portfolio Summary ---\n");
        prompt.append(String.format("Total Investment Cost: $%.2f\n", totalInvestmentCost));
        prompt.append(String.format("Current Portfolio Value: $%.2f\n", totalPortfolioValue));
        prompt.append(String.format("Total Profit/Loss: $%.2f (%.2f%%)\n", totalProfitLoss, totalProfitLossPercentage));
        prompt.append("\n");

        // Add investment context from centralized strategy
        prompt.append("--- Investment Context ---\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());
        
        // Add risk management guidelines
        prompt.append(investmentStrategyService.getRiskManagementSection());
        
        // Add analysis framework for consistency
        prompt.append(investmentStrategyService.getAnalysisFrameworkSection());
        
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

    /**
     * Build technical analysis summary from market data
     */
    private String buildTechnicalAnalysis(MarketData marketData) {
        if (marketData == null) {
            return "N/A";
        }

        StringBuilder analysis = new StringBuilder();

        // RSI Analysis
        if (marketData.getRsi() != null) {
            double rsi = marketData.getRsi();
            String rsiStatus = rsi > 70 ? "Overbought" : (rsi < 30 ? "Oversold" : "Neutral");
            analysis.append(String.format("RSI: %.1f (%s), ", rsi, rsiStatus));
        }

        // Moving Average Analysis
        if (marketData.getSma20() != null && marketData.getSma50() != null) {
            double currentPrice = marketData.getCurrentPrice();
            String maStatus = "";

            if (currentPrice > marketData.getSma20() && currentPrice > marketData.getSma50()) {
                maStatus = "Above key MAs (Bullish)";
            } else if (currentPrice < marketData.getSma20() && currentPrice < marketData.getSma50()) {
                maStatus = "Below key MAs (Bearish)";
            } else {
                maStatus = "Mixed MA signals";
            }

            analysis.append(String.format("MA Status: %s, ", maStatus));
        }

        // MACD Analysis
        if (marketData.getMacd() != null && marketData.getMacdSignal() != null) {
            String macdStatus = marketData.getMacd() > marketData.getMacdSignal() ? "Bullish" : "Bearish";
            analysis.append(String.format("MACD: %s", macdStatus));
        }

        return analysis.length() > 0 ? analysis.toString() : "Limited technical data available";
    }

    /**
     * Parse AI response from risk and opportunity analysis into structured data
     */
    public Map<String, Object> parseRiskOpportunityResponse(String response) {
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
                List<Map<String, String>> individualAnalysis = parseIndividualAnalysis(responseNode.get("individual_analysis"));
                parsedData.put("individual_analysis", individualAnalysis);
            }

            // Parse portfolio analysis
            if (responseNode.has("portfolio_analysis")) {
                Map<String, Object> portfolioAnalysis = parsePortfolioAnalysis(responseNode.get("portfolio_analysis"));
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

    /**
     * Parse individual coin analysis section
     */
    private List<Map<String, String>> parseIndividualAnalysis(JsonNode individualArray) {
        List<Map<String, String>> individualAnalysis = new ArrayList<>();

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

        return individualAnalysis;
    }

    /**
     * Parse portfolio-level analysis section
     */
    private Map<String, Object> parsePortfolioAnalysis(JsonNode portfolioNode) {
        Map<String, Object> portfolioAnalysis = new HashMap<>();

        // Parse sector diversification
        if (portfolioNode.has("sector_diversification")) {
            Map<String, String> sectorDiv = parseSectorDiversification(portfolioNode.get("sector_diversification"));
            portfolioAnalysis.put("sector_diversification", sectorDiv);
        }

        // Parse basic portfolio fields
        portfolioAnalysis.put("correlation_risks", portfolioNode.has("correlation_risks") ? portfolioNode.get("correlation_risks").asText() : "");
        portfolioAnalysis.put("overexposure_concerns", portfolioNode.has("overexposure_concerns") ? portfolioNode.get("overexposure_concerns").asText() : "");
        portfolioAnalysis.put("diversification_score", portfolioNode.has("diversification_score") ? portfolioNode.get("diversification_score").asText() : "FAIR");
        portfolioAnalysis.put("risk_reward_balance", portfolioNode.has("risk_reward_balance") ? portfolioNode.get("risk_reward_balance").asText() : "");

        // Parse recommended adjustments
        if (portfolioNode.has("recommended_adjustments")) {
            List<Map<String, String>> adjustments = parseRecommendedAdjustments(portfolioNode.get("recommended_adjustments"));
            portfolioAnalysis.put("recommended_adjustments", adjustments);
        }

        // Parse missing sectors
        if (portfolioNode.has("missing_sectors")) {
            List<String> missingSectors = parseMissingSectors(portfolioNode.get("missing_sectors"));
            portfolioAnalysis.put("missing_sectors", missingSectors);
        }

        return portfolioAnalysis;
    }

    /**
     * Parse sector diversification breakdown
     */
    private Map<String, String> parseSectorDiversification(JsonNode sectorNode) {
        Map<String, String> sectorDiv = new HashMap<>();
        sectorDiv.put("layer1_exposure", sectorNode.has("layer1_exposure") ? sectorNode.get("layer1_exposure").asText() : "");
        sectorDiv.put("layer2_exposure", sectorNode.has("layer2_exposure") ? sectorNode.get("layer2_exposure").asText() : "");
        sectorDiv.put("defi_exposure", sectorNode.has("defi_exposure") ? sectorNode.get("defi_exposure").asText() : "");
        sectorDiv.put("ai_exposure", sectorNode.has("ai_exposure") ? sectorNode.get("ai_exposure").asText() : "");
        sectorDiv.put("other_sectors", sectorNode.has("other_sectors") ? sectorNode.get("other_sectors").asText() : "");
        return sectorDiv;
    }

    /**
     * Parse recommended portfolio adjustments
     */
    private List<Map<String, String>> parseRecommendedAdjustments(JsonNode adjustmentsArray) {
        List<Map<String, String>> adjustments = new ArrayList<>();

        for (JsonNode adjustment : adjustmentsArray) {
            Map<String, String> adj = new HashMap<>();
            adj.put("action", adjustment.has("action") ? adjustment.get("action").asText() : "");
            adj.put("rationale", adjustment.has("rationale") ? adjustment.get("rationale").asText() : "");
            adj.put("priority", adjustment.has("priority") ? adjustment.get("priority").asText() : "MEDIUM");
            adjustments.add(adj);
        }

        return adjustments;
    }

    /**
     * Parse missing sectors recommendations
     */
    private List<String> parseMissingSectors(JsonNode missingSectorsArray) {
        List<String> missingSectors = new ArrayList<>();
        
        for (JsonNode sector : missingSectorsArray) {
            missingSectors.add(sector.asText());
        }
        
        return missingSectors;
    }
}
