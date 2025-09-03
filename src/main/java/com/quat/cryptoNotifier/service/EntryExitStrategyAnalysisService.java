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
 * Service dedicated to entry and exit strategy analysis functionality.
 * 
 * PURPOSE:
 * This service is responsible for providing comprehensive entry and exit strategy recommendations by:
 * 1. Analyzing optimal entry points and market timing for each asset
 * 2. Creating detailed exit strategies with profit-taking levels and stop-losses
 * 3. Assessing current market context and sector rotation dynamics
 * 4. Providing strategic recommendations for position management
 * 5. Creating actionable trading plans with risk management
 * 
 * The analysis helps users make informed decisions about when to enter positions,
 * how to manage existing holdings, and when to exit for optimal risk-adjusted returns.
 * It considers both technical factors and fundamental market dynamics to provide
 * comprehensive trading strategies.
 */
@Service
public class EntryExitStrategyAnalysisService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private DataProviderService dataProviderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build a comprehensive entry and exit strategy analysis prompt.
     * 
     * This method creates a detailed prompt that asks the AI to analyze each holding for:
     * - Entry strategy with optimal zones, DCA approach, and confirmation signals
     * - Exit strategy with profit-taking levels, stop-losses, and invalidation conditions
     * - Market context analysis and timing considerations
     * - Strategic recommendations for portfolio management
     */
    public String buildEntryExitStrategyPrompt(List<Holding> holdings) {
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

        // Add centralized investment strategy context
        prompt.append("--- Context & Preferences ---\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());

        // Add output format specification
        prompt.append("--- Output Format ---\n");
        prompt.append("Please provide your analysis in the following structured JSON format:\n");
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

    /**
     * Parse AI response from entry/exit strategy analysis into structured data.
     * 
     * This method processes the AI's JSON response and extracts all relevant information
     * including individual asset strategies, portfolio management recommendations,
     * entry/exit zones, profit-taking levels, and risk management guidelines.
     */
    public Map<String, Object> parseEntryExitStrategyResponse(String response) {
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

            // Parse strategies array
            List<Map<String, Object>> strategies = new ArrayList<>();
            if (root.has("strategies") && root.get("strategies").isArray()) {
                for (JsonNode s : root.get("strategies")) {
                    Map<String, Object> strategyData = parseStrategyData(s);
                    strategies.add(strategyData);
                }
            }
            parsed.put("strategies", strategies);

            // Parse portfolio management section
            Map<String, String> portfolio = new HashMap<>();
            if (root.has("portfolio") && root.get("portfolio").isObject()) {
                JsonNode p = root.get("portfolio");
                portfolio.put("cash_buffer", p.has("cash_buffer") ? p.get("cash_buffer").asText() : "");
                portfolio.put("rebalance_trigger", p.has("rebalance_trigger") ? p.get("rebalance_trigger").asText() : "");
                portfolio.put("risk_guardrails", p.has("risk_guardrails") ? p.get("risk_guardrails").asText() : "");
            }
            parsed.put("portfolio", portfolio);

            // Optional: minimal backward-compatibility handling
            if (strategies.isEmpty() && root.has("individual_strategies")) {
                System.out.println("DEBUG: Falling back to individual_strategies format");
                // Handle legacy format if needed
            }

        } catch (Exception e) {
            System.err.println("Error parsing entry/exit strategy response: " + e.getMessage());
            e.printStackTrace();

            // Safe defaults on failure
            parsed.put("strategies", new ArrayList<>());
            parsed.put("portfolio", new HashMap<>());
        }

        System.out.println("DEBUG: Final parsed data: " + parsed);
        return parsed;
    }

    /**
     * Parse individual strategy data from JSON node.
     */
    private Map<String, Object> parseStrategyData(JsonNode strategyNode) {
        Map<String, Object> strategyData = new HashMap<>();
        
        strategyData.put("symbol", strategyNode.has("symbol") ? strategyNode.get("symbol").asText() : "");
        strategyData.put("action", strategyNode.has("action") ? strategyNode.get("action").asText() : "");

        // Parse entry strategy
        Map<String, Object> entry = parseEntryStrategy(strategyNode.get("entry"));
        strategyData.put("entry", entry);

        // Parse exit strategy
        Map<String, Object> exit = parseExitStrategy(strategyNode.get("exit"));
        strategyData.put("exit", exit);

        strategyData.put("notes", strategyNode.has("notes") ? strategyNode.get("notes").asText() : "");
        
        return strategyData;
    }

    /**
     * Parse entry strategy section from JSON node.
     */
    private Map<String, Object> parseEntryStrategy(JsonNode entryNode) {
        Map<String, Object> entry = new HashMap<>();
        
        if (entryNode != null && entryNode.isObject()) {
            // Parse entry zones
            List<String> zones = new ArrayList<>();
            if (entryNode.has("zones") && entryNode.get("zones").isArray()) {
                for (JsonNode zone : entryNode.get("zones")) {
                    zones.add(zone.asText());
                }
            }
            entry.put("zones", zones);
            
            entry.put("dca", entryNode.has("dca") ? entryNode.get("dca").asText() : "");
            entry.put("confirmation", entryNode.has("confirmation") ? entryNode.get("confirmation").asText() : "");
        }
        
        return entry;
    }

    /**
     * Parse exit strategy section from JSON node.
     */
    private Map<String, Object> parseExitStrategy(JsonNode exitNode) {
        Map<String, Object> exit = new HashMap<>();
        
        if (exitNode != null && exitNode.isObject()) {
            // Parse take profit levels
            List<Map<String, String>> takeProfits = new ArrayList<>();
            if (exitNode.has("take_profits") && exitNode.get("take_profits").isArray()) {
                for (JsonNode tp : exitNode.get("take_profits")) {
                    Map<String, String> tpMap = new HashMap<>();
                    tpMap.put("level", tp.has("level") ? tp.get("level").asText() : "");
                    tpMap.put("sell_pct", tp.has("sell_pct") ? tp.get("sell_pct").asText() : "");
                    takeProfits.add(tpMap);
                }
            }
            exit.put("take_profits", takeProfits);

            // Parse stop loss
            Map<String, String> stop = new HashMap<>();
            if (exitNode.has("stop") && exitNode.get("stop").isObject()) {
                JsonNode stopNode = exitNode.get("stop");
                stop.put("type", stopNode.has("type") ? stopNode.get("type").asText() : "");
                stop.put("value", stopNode.has("value") ? stopNode.get("value").asText() : "");
            }
            exit.put("stop", stop);

            // Parse trailing stop
            Map<String, String> trailing = new HashMap<>();
            if (exitNode.has("trailing") && exitNode.get("trailing").isObject()) {
                JsonNode trailingNode = exitNode.get("trailing");
                trailing.put("type", trailingNode.has("type") ? trailingNode.get("type").asText() : "");
                trailing.put("value", trailingNode.has("value") ? trailingNode.get("value").asText() : "");
            }
            exit.put("trailing", trailing);

            // Parse invalidation conditions
            List<String> invalidations = new ArrayList<>();
            if (exitNode.has("invalidations") && exitNode.get("invalidations").isArray()) {
                for (JsonNode invalidation : exitNode.get("invalidations")) {
                    invalidations.add(invalidation.asText());
                }
            }
            exit.put("invalidations", invalidations);
        }
        
        return exit;
    }
}
