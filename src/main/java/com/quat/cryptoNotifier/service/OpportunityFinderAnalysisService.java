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
 * Service dedicated to opportunity finder analysis functionality.
 * 
 * PURPOSE:
 * This service is responsible for identifying investment opportunities within a crypto portfolio by:
 * 1. Analyzing fundamental strength of existing holdings
 * 2. Identifying speculative/risky positions
 * 3. Finding diversification gaps and opportunities
 * 4. Recommending position adjustments (exits, reductions, additions)
 * 5. Creating actionable investment plans
 * 
 * The analysis provides a comprehensive view of portfolio optimization opportunities,
 * helping users make informed decisions about which assets to hold, reduce, or add
 * to improve their portfolio's risk-adjusted returns and diversification.
 */
@Service
public class OpportunityFinderAnalysisService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private DataProviderService dataProviderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build a comprehensive opportunity finder analysis prompt with current market data.
     *
     * This method creates a detailed prompt that asks the AI to analyze the portfolio for:
     * - Fundamental strength of each asset
     * - Risk assessment and speculative elements
     * - Diversification opportunities and missing sectors
     * - Exit/reduction recommendations
     * - Actionable investment plan
     */
    public String buildOpportunityFinderPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("This analysis is based on my overall investment strategy and current portfolio holdings.\n\n");
        prompt.append(investmentStrategyService.getCompleteInvestmentStrategySection());
        prompt.append("Analyze my crypto portfolio and identify:\n");
        prompt.append("- Which coins have the strongest long-term fundamentals.\n");
        prompt.append("- Which ones are speculative or risky.\n");
        prompt.append("- Where I could add new assets to improve diversification.\n");
        prompt.append("- Which coins I should reduce or exit.\n\n");
        
        prompt.append("Portfolio data with current market information:\n\n");

        // Calculate total portfolio value with current market data
        double totalPortfolioValue = 0;
        double totalInvestmentCost = 0;

        for (Holding holding : holdings) {
            if ("USDT".equals(holding.getSymbol()) || "USDC".equals(holding.getSymbol()) || "BUSD".equals(holding.getSymbol())) {
                totalInvestmentCost += holding.getTotalAvgCost();
                continue; // Skip stablecoins for market value calculation but include in cost
            }

            // Get current market data
            MarketData marketData = null;
            double currentPrice = 0;
            double currentValue = 0;

            try {
                marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    currentPrice = marketData.getCurrentPrice();
                    currentValue = holding.getHoldings() * currentPrice;
                }
            } catch (Exception e) {
                System.err.println("Error fetching market data for " + holding.getSymbol() + ": " + e.getMessage());
                currentPrice = holding.getAveragePrice(); // Fallback to average price
                currentValue = holding.getTotalAvgCost();
            }

            totalPortfolioValue += currentValue;
            totalInvestmentCost += holding.getTotalAvgCost();
        }
        
        // Add stablecoin value to total portfolio value
        for (Holding holding : holdings) {
            if ("USDT".equals(holding.getSymbol()) || "USDC".equals(holding.getSymbol()) || "BUSD".equals(holding.getSymbol())) {
                totalPortfolioValue += holding.getTotalAvgCost(); // Stablecoins at face value
            }
        }

        // Add detailed portfolio information with market data
        for (Holding holding : holdings) {
            double currentWeight = 0;
            double currentPrice = 0;
            double currentValue = 0;
            double profitLoss = 0;
            double profitLossPercentage = 0;
            String technicalAnalysis = "N/A";

            if ("USDT".equals(holding.getSymbol()) || "USDC".equals(holding.getSymbol()) || "BUSD".equals(holding.getSymbol())) {
                // Handle stablecoins
                currentPrice = 1.0;
                currentValue = holding.getTotalAvgCost();
                currentWeight = (currentValue / totalPortfolioValue) * 100;
            } else {
                // Get market data for cryptocurrencies
                try {
                    MarketData marketData = dataProviderService.getMarketData(holding.getId());
                    if (marketData != null) {
                        currentPrice = marketData.getCurrentPrice();
                        currentValue = holding.getHoldings() * currentPrice;
                        profitLoss = currentValue - holding.getTotalAvgCost();
                        profitLossPercentage = ((currentPrice - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
                        technicalAnalysis = buildTechnicalAnalysis(marketData);
                        currentWeight = (currentValue / totalPortfolioValue) * 100;
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching market data for " + holding.getSymbol() + ": " + e.getMessage());
                    currentPrice = holding.getAveragePrice();
                    currentValue = holding.getTotalAvgCost();
                    currentWeight = (currentValue / totalPortfolioValue) * 100;
                }
            }

            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Sector: %s\n", holding.getSector() != null ? holding.getSector() : "Unknown"));
            prompt.append(String.format("Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            prompt.append(String.format("Current Price: $%.2f\n", currentPrice));
            prompt.append(String.format("Current Value: $%.2f\n", currentValue));
            prompt.append(String.format("Current Weight: %.1f%% of portfolio\n", currentWeight));
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

            // Calculate potential returns from current price
            if (currentPrice > 0) {
                double targetReturn3M = ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100;
                double targetReturnLong = ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100;
                prompt.append(String.format("Upside to 3M Target: %.1f%%\n", targetReturn3M));
                prompt.append(String.format("Upside to Long Target: %.1f%%\n", targetReturnLong));
            }

            // Performance since purchase
            if (!("USDT".equals(holding.getSymbol()) || "USDC".equals(holding.getSymbol()) || "BUSD".equals(holding.getSymbol()))) {
                double totalReturnFromEntry = ((holding.getAveragePrice() - holding.getExpectedEntry()) / holding.getExpectedEntry()) * 100;
                prompt.append(String.format("Entry Performance: %.1f%% vs expected entry\n", totalReturnFromEntry));
            }
            prompt.append("\n");
        }
        
        // Add portfolio summary with current market performance
        double totalProfitLoss = totalPortfolioValue - totalInvestmentCost;
        double totalProfitLossPercentage = totalInvestmentCost > 0 ? (totalProfitLoss / totalInvestmentCost) * 100 : 0;

        prompt.append("--- Portfolio Summary ---\n");
        prompt.append(String.format("Total Investment Cost: $%.2f\n", totalInvestmentCost));
        prompt.append(String.format("Current Portfolio Value: $%.2f\n", totalPortfolioValue));
        prompt.append(String.format("Total Profit/Loss: $%.2f (%.2f%%)\n", totalProfitLoss, totalProfitLossPercentage));
        prompt.append("\n");

        // Add centralized analysis framework and investment strategy
        prompt.append(investmentStrategyService.getAnalysisFrameworkSection());
        prompt.append(investmentStrategyService.getDecisionCriteriaSection());
        
        prompt.append("--- Investment Context ---\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());
        prompt.append("Portfolio size: $" + String.format("%.0f", totalPortfolioValue) + "\n");
        prompt.append("Market conditions: Consider current crypto market cycle and macro environment\n\n");
        
        prompt.append("--- Output Format ---\n");
        prompt.append("Please provide your opportunity analysis in the following structured JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"portfolio_grade\": \"A|B|C|D|F\",\n");
        prompt.append("  \"opportunity_summary\": \"Brief summary of key opportunities and concerns\",\n");
        prompt.append("  \"fundamental_strength_analysis\": {\n");
        prompt.append("    \"strongest_coins\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"BTC\",\n");
        prompt.append("        \"strength_score\": \"EXCELLENT|STRONG|GOOD|FAIR|WEAK\",\n");
        prompt.append("        \"key_strengths\": \"Primary fundamental advantages\",\n");
        prompt.append("        \"growth_drivers\": \"Key catalysts for future growth\",\n");
        prompt.append("        \"recommendation\": \"HOLD|ACCUMULATE|MAINTAIN\",\n");
        prompt.append("        \"rationale\": \"Reasoning for the assessment\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"moderate_coins\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"SOL\",\n");
        prompt.append("        \"strength_score\": \"GOOD|FAIR\",\n");
        prompt.append("        \"key_strengths\": \"Notable advantages\",\n");
        prompt.append("        \"concerns\": \"Areas of concern or uncertainty\",\n");
        prompt.append("        \"recommendation\": \"HOLD|MONITOR|REDUCE\",\n");
        prompt.append("        \"rationale\": \"Reasoning for the assessment\"\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"risk_assessment\": {\n");
        prompt.append("    \"speculative_coins\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"MEME\",\n");
        prompt.append("        \"risk_level\": \"VERY_HIGH|HIGH|MODERATE\",\n");
        prompt.append("        \"risk_factors\": \"Primary risk concerns\",\n");
        prompt.append("        \"speculative_elements\": \"What makes it speculative\",\n");
        prompt.append("        \"recommendation\": \"EXIT|REDUCE|MONITOR\",\n");
        prompt.append("        \"timeline\": \"When to take action\",\n");
        prompt.append("        \"rationale\": \"Reasoning for the assessment\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"portfolio_risks\": \"Overall portfolio risk assessment\",\n");
        prompt.append("    \"concentration_concerns\": \"Areas of excessive concentration\"\n");
        prompt.append("  },\n");
        prompt.append("  \"diversification_opportunities\": {\n");
        prompt.append("    \"missing_sectors\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"sector\": \"AI/Machine Learning\",\n");
        prompt.append("        \"rationale\": \"Why this sector would improve the portfolio\",\n");
        prompt.append("        \"suggested_assets\": [\"FET\", \"RNDR\", \"TAO\"],\n");
        prompt.append("        \"allocation_suggestion\": \"5-10%\",\n");
        prompt.append("        \"priority\": \"HIGH|MEDIUM|LOW\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"underrepresented_areas\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"area\": \"Stablecoins/Yield\",\n");
        prompt.append("        \"current_allocation\": \"11.9%\",\n");
        prompt.append("        \"suggested_allocation\": \"10-15%\",\n");
        prompt.append("        \"benefits\": \"Risk reduction and yield generation\",\n");
        prompt.append("        \"suggested_assets\": [\"USDC\", \"stETH\", \"DAI\"]\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"market_cap_gaps\": \"Assessment of market cap distribution\"\n");
        prompt.append("  },\n");
        prompt.append("  \"exit_reduce_recommendations\": {\n");
        prompt.append("    \"immediate_exits\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"RISKY\",\n");
        prompt.append("        \"reason\": \"Deteriorating fundamentals or excessive risk\",\n");
        prompt.append("        \"urgency\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("        \"exit_strategy\": \"How to exit (gradual vs immediate)\",\n");
        prompt.append("        \"reinvestment_suggestion\": \"Where to redeploy capital\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"position_reductions\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"symbol\": \"OVERWEIGHT\",\n");
        prompt.append("        \"current_weight\": \"25%\",\n");
        prompt.append("        \"suggested_weight\": \"15-20%\",\n");
        prompt.append("        \"reduction_amount\": \"20-40% of position\",\n");
        prompt.append("        \"reason\": \"Concentration risk or profit-taking\",\n");
        prompt.append("        \"timing\": \"When to reduce the position\"\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"action_plan\": {\n");
        prompt.append("    \"immediate_actions\": [\"List of actions to take within 1 month\"],\n");
        prompt.append("    \"medium_term_actions\": [\"List of actions for 1-6 months\"],\n");
        prompt.append("    \"long_term_strategy\": \"Overall strategic direction for the portfolio\"\n");
        prompt.append("  },\n");
        prompt.append("  \"market_timing_considerations\": \"Current market conditions and timing factors\"\n");
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
     * Parse AI response from opportunity finder analysis into structured data.
     * 
     * This method processes the AI's JSON response and extracts all relevant information
     * including portfolio grading, fundamental analysis, risk assessment, diversification
     * opportunities, exit recommendations, and actionable plans.
     */
    public Map<String, Object> parseOpportunityFinderResponse(String response) {
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
            parsedData.put("portfolio_grade", responseNode.has("portfolio_grade") ? responseNode.get("portfolio_grade").asText() : "C");
            parsedData.put("opportunity_summary", responseNode.has("opportunity_summary") ? responseNode.get("opportunity_summary").asText() : "Opportunity analysis completed");

            // Parse fundamental strength analysis
            if (responseNode.has("fundamental_strength_analysis")) {
                Map<String, Object> strengthAnalysis = parseFundamentalStrengthAnalysis(responseNode.get("fundamental_strength_analysis"));
                parsedData.put("fundamental_strength_analysis", strengthAnalysis);
            }

            // Parse risk assessment
            if (responseNode.has("risk_assessment")) {
                Map<String, Object> riskAssessment = parseRiskAssessment(responseNode.get("risk_assessment"));
                parsedData.put("risk_assessment", riskAssessment);
            }

            // Parse diversification opportunities
            if (responseNode.has("diversification_opportunities")) {
                Map<String, Object> diversification = parseDiversificationOpportunities(responseNode.get("diversification_opportunities"));
                parsedData.put("diversification_opportunities", diversification);
            }

            // Parse exit/reduce recommendations
            if (responseNode.has("exit_reduce_recommendations")) {
                Map<String, Object> exitRecommendations = parseExitReduceRecommendations(responseNode.get("exit_reduce_recommendations"));
                parsedData.put("exit_reduce_recommendations", exitRecommendations);
            }

            // Parse action plan
            if (responseNode.has("action_plan")) {
                Map<String, Object> actionPlan = parseActionPlan(responseNode.get("action_plan"));
                parsedData.put("action_plan", actionPlan);
            }

            // Parse market timing considerations with markdown formatting
            String marketTiming = responseNode.has("market_timing_considerations") ? responseNode.get("market_timing_considerations").asText() : "";
            parsedData.put("market_timing_considerations", convertMarkdownToHtml(marketTiming));

        } catch (Exception e) {
            System.err.println("Error parsing opportunity finder response: " + e.getMessage());
            e.printStackTrace();

            // Set default values if parsing fails
            parsedData.put("portfolio_grade", "C");
            parsedData.put("opportunity_summary", "Unable to parse opportunity analysis due to API response parsing error");
            parsedData.put("fundamental_strength_analysis", new HashMap<>());
            parsedData.put("risk_assessment", new HashMap<>());
            parsedData.put("diversification_opportunities", new HashMap<>());
            parsedData.put("exit_reduce_recommendations", new HashMap<>());
            parsedData.put("action_plan", new HashMap<>());
            parsedData.put("market_timing_considerations", "Market analysis unavailable");
        }

        return parsedData;
    }

    /**
     * Convert markdown-style bold formatting (**text**) to HTML bold tags and add line breaks
     */
    private String convertMarkdownToHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Convert **text** to <strong>text</strong>
        String processedText = text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        // Add line breaks after numbered points for better formatting
        processedText = processedText.replaceAll("(\\d+\\. \\*\\*[^*]+\\*\\*[^\\d]*?)(?=\\d+\\. \\*\\*|$)", "$1<br><br>");

        return processedText;
    }

    /**
     * Parse fundamental strength analysis section
     */
    private Map<String, Object> parseFundamentalStrengthAnalysis(JsonNode strengthNode) {
        Map<String, Object> strengthAnalysis = new HashMap<>();

        // Parse strongest coins
        if (strengthNode.has("strongest_coins")) {
            List<Map<String, String>> strongest = new ArrayList<>();
            JsonNode strongestArray = strengthNode.get("strongest_coins");

            for (JsonNode coin : strongestArray) {
                Map<String, String> coinData = new HashMap<>();
                coinData.put("symbol", coin.has("symbol") ? coin.get("symbol").asText() : "");
                coinData.put("strength_score", coin.has("strength_score") ? coin.get("strength_score").asText() : "GOOD");
                coinData.put("key_strengths", coin.has("key_strengths") ? coin.get("key_strengths").asText() : "");
                coinData.put("growth_drivers", coin.has("growth_drivers") ? coin.get("growth_drivers").asText() : "");
                coinData.put("recommendation", coin.has("recommendation") ? coin.get("recommendation").asText() : "HOLD");
                coinData.put("rationale", coin.has("rationale") ? coin.get("rationale").asText() : "");
                strongest.add(coinData);
            }
            strengthAnalysis.put("strongest_coins", strongest);
        }

        // Parse moderate coins
        if (strengthNode.has("moderate_coins")) {
            List<Map<String, String>> moderate = new ArrayList<>();
            JsonNode moderateArray = strengthNode.get("moderate_coins");

            for (JsonNode coin : moderateArray) {
                Map<String, String> coinData = new HashMap<>();
                coinData.put("symbol", coin.has("symbol") ? coin.get("symbol").asText() : "");
                coinData.put("strength_score", coin.has("strength_score") ? coin.get("strength_score").asText() : "FAIR");
                coinData.put("key_strengths", coin.has("key_strengths") ? coin.get("key_strengths").asText() : "");
                coinData.put("concerns", coin.has("concerns") ? coin.get("concerns").asText() : "");
                coinData.put("recommendation", coin.has("recommendation") ? coin.get("recommendation").asText() : "HOLD");
                coinData.put("rationale", coin.has("rationale") ? coin.get("rationale").asText() : "");
                moderate.add(coinData);
            }
            strengthAnalysis.put("moderate_coins", moderate);
        }

        return strengthAnalysis;
    }

    /**
     * Parse risk assessment section
     */
    private Map<String, Object> parseRiskAssessment(JsonNode riskNode) {
        Map<String, Object> riskAssessment = new HashMap<>();

        // Parse speculative coins
        if (riskNode.has("speculative_coins")) {
            List<Map<String, String>> speculative = new ArrayList<>();
            JsonNode speculativeArray = riskNode.get("speculative_coins");

            for (JsonNode coin : speculativeArray) {
                Map<String, String> coinData = new HashMap<>();
                coinData.put("symbol", coin.has("symbol") ? coin.get("symbol").asText() : "");
                coinData.put("risk_level", coin.has("risk_level") ? coin.get("risk_level").asText() : "HIGH");
                coinData.put("risk_factors", coin.has("risk_factors") ? coin.get("risk_factors").asText() : "");
                coinData.put("speculative_elements", coin.has("speculative_elements") ? coin.get("speculative_elements").asText() : "");
                coinData.put("recommendation", coin.has("recommendation") ? coin.get("recommendation").asText() : "MONITOR");
                coinData.put("timeline", coin.has("timeline") ? coin.get("timeline").asText() : "");
                coinData.put("rationale", coin.has("rationale") ? coin.get("rationale").asText() : "");
                speculative.add(coinData);
            }
            riskAssessment.put("speculative_coins", speculative);
        }

        // Apply markdown conversion to portfolio_risks and concentration_concerns
        riskAssessment.put("portfolio_risks", riskNode.has("portfolio_risks") ? convertMarkdownToHtml(riskNode.get("portfolio_risks").asText()) : "");
        riskAssessment.put("concentration_concerns", riskNode.has("concentration_concerns") ? convertMarkdownToHtml(riskNode.get("concentration_concerns").asText()) : "");

        return riskAssessment;
    }

    /**
     * Parse diversification opportunities section
     */
    private Map<String, Object> parseDiversificationOpportunities(JsonNode diversificationNode) {
        Map<String, Object> diversification = new HashMap<>();

        // Parse missing sectors
        if (diversificationNode.has("missing_sectors")) {
            List<Map<String, Object>> missingSectors = new ArrayList<>();
            JsonNode missingSectorsArray = diversificationNode.get("missing_sectors");

            for (JsonNode sector : missingSectorsArray) {
                Map<String, Object> sectorData = new HashMap<>();
                sectorData.put("sector", sector.has("sector") ? sector.get("sector").asText() : "");
                sectorData.put("rationale", sector.has("rationale") ? sector.get("rationale").asText() : "");
                sectorData.put("allocation_suggestion", sector.has("allocation_suggestion") ? sector.get("allocation_suggestion").asText() : "5%");
                sectorData.put("priority", sector.has("priority") ? sector.get("priority").asText() : "MEDIUM");

                // Parse suggested assets
                if (sector.has("suggested_assets")) {
                    List<String> suggestedAssets = new ArrayList<>();
                    JsonNode assetsArray = sector.get("suggested_assets");
                    for (JsonNode asset : assetsArray) {
                        suggestedAssets.add(asset.asText());
                    }
                    sectorData.put("suggested_assets", suggestedAssets);
                }

                missingSectors.add(sectorData);
            }
            diversification.put("missing_sectors", missingSectors);
        }

        // Parse underrepresented areas
        if (diversificationNode.has("underrepresented_areas")) {
            List<Map<String, Object>> underrepresented = new ArrayList<>();
            JsonNode underrepresentedArray = diversificationNode.get("underrepresented_areas");

            for (JsonNode area : underrepresentedArray) {
                Map<String, Object> areaData = new HashMap<>();
                areaData.put("area", area.has("area") ? area.get("area").asText() : "");
                areaData.put("current_allocation", area.has("current_allocation") ? area.get("current_allocation").asText() : "0%");
                areaData.put("suggested_allocation", area.has("suggested_allocation") ? area.get("suggested_allocation").asText() : "5%");
                areaData.put("benefits", area.has("benefits") ? area.get("benefits").asText() : "");

                // Parse suggested assets
                if (area.has("suggested_assets")) {
                    List<String> suggestedAssets = new ArrayList<>();
                    JsonNode assetsArray = area.get("suggested_assets");
                    for (JsonNode asset : assetsArray) {
                        suggestedAssets.add(asset.asText());
                    }
                    areaData.put("suggested_assets", suggestedAssets);
                }

                underrepresented.add(areaData);
            }
            diversification.put("underrepresented_areas", underrepresented);
        }

        diversification.put("market_cap_gaps", diversificationNode.has("market_cap_gaps") ? diversificationNode.get("market_cap_gaps").asText() : "");
        
        return diversification;
    }

    /**
     * Parse exit and reduction recommendations section
     */
    private Map<String, Object> parseExitReduceRecommendations(JsonNode exitNode) {
        Map<String, Object> exitRecommendations = new HashMap<>();

        // Parse immediate exits
        if (exitNode.has("immediate_exits")) {
            List<Map<String, String>> immediateExits = new ArrayList<>();
            JsonNode exitsArray = exitNode.get("immediate_exits");

            for (JsonNode exit : exitsArray) {
                Map<String, String> exitData = new HashMap<>();
                exitData.put("symbol", exit.has("symbol") ? exit.get("symbol").asText() : "");
                exitData.put("reason", exit.has("reason") ? exit.get("reason").asText() : "");
                exitData.put("urgency", exit.has("urgency") ? exit.get("urgency").asText() : "MEDIUM");
                exitData.put("exit_strategy", exit.has("exit_strategy") ? exit.get("exit_strategy").asText() : "");
                exitData.put("reinvestment_suggestion", exit.has("reinvestment_suggestion") ? exit.get("reinvestment_suggestion").asText() : "");
                immediateExits.add(exitData);
            }
            exitRecommendations.put("immediate_exits", immediateExits);
        }

        // Parse position reductions
        if (exitNode.has("position_reductions")) {
            List<Map<String, String>> reductions = new ArrayList<>();
            JsonNode reductionsArray = exitNode.get("position_reductions");

            for (JsonNode reduction : reductionsArray) {
                Map<String, String> reductionData = new HashMap<>();
                reductionData.put("symbol", reduction.has("symbol") ? reduction.get("symbol").asText() : "");
                reductionData.put("current_weight", reduction.has("current_weight") ? reduction.get("current_weight").asText() : "");
                reductionData.put("suggested_weight", reduction.has("suggested_weight") ? reduction.get("suggested_weight").asText() : "");
                reductionData.put("reduction_amount", reduction.has("reduction_amount") ? reduction.get("reduction_amount").asText() : "");
                reductionData.put("reason", reduction.has("reason") ? reduction.get("reason").asText() : "");
                reductionData.put("timing", reduction.has("timing") ? reduction.get("timing").asText() : "");
                reductions.add(reductionData);
            }
            exitRecommendations.put("position_reductions", reductions);
        }

        return exitRecommendations;
    }

    /**
     * Parse action plan section
     */
    private Map<String, Object> parseActionPlan(JsonNode actionNode) {
        Map<String, Object> actionPlan = new HashMap<>();

        // Parse immediate actions
        if (actionNode.has("immediate_actions")) {
            List<String> immediateActions = new ArrayList<>();
            JsonNode actionsArray = actionNode.get("immediate_actions");
            for (JsonNode action : actionsArray) {
                immediateActions.add(action.asText());
            }
            actionPlan.put("immediate_actions", immediateActions);
        }

        // Parse medium term actions
        if (actionNode.has("medium_term_actions")) {
            List<String> mediumTermActions = new ArrayList<>();
            JsonNode actionsArray = actionNode.get("medium_term_actions");
            for (JsonNode action : actionsArray) {
                mediumTermActions.add(action.asText());
            }
            actionPlan.put("medium_term_actions", mediumTermActions);
        }

        actionPlan.put("long_term_strategy", actionNode.has("long_term_strategy") ? actionNode.get("long_term_strategy").asText() : "");
        
        return actionPlan;
    }
}
