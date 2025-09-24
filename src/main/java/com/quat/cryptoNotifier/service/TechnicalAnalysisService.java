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
 * Service dedicated to comprehensive technical analysis functionality.
 *
 * PURPOSE:
 * This service provides detailed technical analysis for cryptocurrency holdings by:
 * 1. Analyzing key technical indicators (RSI, MACD, Moving Averages, etc.)
 * 2. Identifying chart patterns and market structures
 * 3. Calculating support and resistance levels
 * 4. Providing momentum and trend analysis
 * 5. Generating technical-based trading advice and signals
 * 6. Creating comprehensive technical analysis tables with actionable insights
 *
 * The analysis focuses purely on price action and technical indicators to provide
 * traders with data-driven insights for making informed trading decisions based on
 * market technicals rather than fundamental analysis.
 */
@Service
public class TechnicalAnalysisService {

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private DataProviderService dataProviderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build a comprehensive technical analysis prompt for all portfolio holdings.
     */
    public String buildTechnicalAnalysisPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== COMPREHENSIVE TECHNICAL ANALYSIS REPORT ===\n");
        prompt.append("⚠️ CRITICAL: KEEP TOTAL RESPONSE UNDER 4000 WORDS. BE PRECISE AND TECHNICAL.\n");
        prompt.append("Perform detailed technical analysis for each cryptocurrency in my portfolio.\n\n");

        prompt.append("ANALYSIS REQUIREMENTS:\n");
        prompt.append("- 📊 TECHNICAL INDICATORS: RSI, MACD, Moving Averages, Volume\n");
        prompt.append("- 📈 CHART PATTERNS: Triangles, Head & Shoulders, Support/Resistance\n");
        prompt.append("- 🎯 PRICE LEVELS: Key support, resistance, breakout levels\n");
        prompt.append("- 📉 MOMENTUM: Trend strength, momentum divergences\n");
        prompt.append("- ⚡ SIGNALS: Buy/Sell signals based on technical confluence\n");
        prompt.append("- 💡 ADVICE: Specific technical-based recommendations\n\n");

        prompt.append("PORTFOLIO TECHNICAL DATA:\n\n");

        // Add detailed technical information for each holding
        for (Holding holding : holdings) {
            prompt.append(String.format("=== %s (%s) TECHNICAL ANALYSIS ===\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Current Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));

            // Fetch current market data for technical analysis
            try {
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    double currentPrice = marketData.getCurrentPrice();
                    prompt.append(String.format("Current Price: $%.2f\n", currentPrice));
                    prompt.append(String.format("24h Change: %.2f%%\n", marketData.getPriceChangePercentage24h()));
                    prompt.append(String.format("24h Volume: $%.0f\n", marketData.getVolume24h()));

                    // Technical indicators if available
                    if (marketData.getRsi() != null) {
                        prompt.append(String.format("RSI (14): %.1f\n", marketData.getRsi()));
                    }
                    if (marketData.getMacd() != null) {
                        prompt.append(String.format("MACD: %.6f\n", marketData.getMacd()));
                    }

                    // Price vs entry levels for technical context
                    double vsAverage = ((currentPrice - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
                    prompt.append(String.format("Price vs Average Buy: %.1f%% (%s)\n", vsAverage,
                        vsAverage > 0 ? "PROFIT" : "LOSS"));

                    // Current P&L for context
                    double currentValue = holding.getHoldings() * currentPrice;
                    double profitLoss = currentValue - holding.getTotalAvgCost();
                    double profitLossPercentage = (profitLoss / holding.getTotalAvgCost()) * 100;
                    prompt.append(String.format("Current P&L: $%.2f (%.1f%%)\n", profitLoss, profitLossPercentage));

                    // Target levels for resistance/support analysis
                    prompt.append("Target Levels for Analysis:\n");
                    prompt.append(String.format("  - 3M Target: $%.2f (%.1f%% potential)\n",
                        holding.getTargetPrice3Month(),
                        ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100));
                    prompt.append(String.format("  - Long Target: $%.2f (%.1f%% potential)\n",
                        holding.getTargetPriceLongTerm(),
                        ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100));
                }
            } catch (Exception e) {
                prompt.append("--- Market Data Unavailable ---\n");
                System.err.println("Could not fetch market data for " + holding.getSymbol() + ": " + e.getMessage());
            }

            prompt.append("\n");
        }

        // Add technical analysis framework
        prompt.append("--- TECHNICAL ANALYSIS FRAMEWORK ---\n");
        prompt.append("For each cryptocurrency, analyze:\n");
        prompt.append("1. TREND ANALYSIS: Overall trend direction and strength\n");
        prompt.append("2. MOMENTUM: RSI levels, MACD signals, momentum divergences\n");
        prompt.append("3. MOVING AVERAGES: 20, 50, 200 MA positioning and crossovers\n");
        prompt.append("4. SUPPORT/RESISTANCE: Key levels based on price history\n");
        prompt.append("5. VOLUME ANALYSIS: Volume trends and price-volume relationship\n");
        prompt.append("6. CHART PATTERNS: Any recognizable patterns forming\n");
        prompt.append("7. TECHNICAL SIGNALS: Buy/sell signals from indicator confluence\n");
        prompt.append("8. RISK LEVELS: Stop-loss and take-profit recommendations\n\n");

        // Add JSON output format specification
        prompt.append("--- JSON OUTPUT FORMAT ---\n");
        prompt.append("Provide detailed technical analysis in this JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"market_technical_overview\": {\n");
        prompt.append("    \"overall_market_trend\": \"BULLISH|BEARISH|SIDEWAYS\",\n");
        prompt.append("    \"market_momentum\": \"STRONG|MODERATE|WEAK\",\n");
        prompt.append("    \"volatility_assessment\": \"HIGH|MODERATE|LOW\",\n");
        prompt.append("    \"sector_rotation\": \"Brief sector analysis\",\n");
        prompt.append("    \"key_market_levels\": \"Important BTC/ETH levels affecting market\"\n");
        prompt.append("  },\n");
        prompt.append("  \"technical_analysis_table\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"BTC\",\n");
        prompt.append("      \"current_price\": \"$92,000\",\n");
        prompt.append("      \"trend_analysis\": {\n");
        prompt.append("        \"primary_trend\": \"BULLISH|BEARISH|SIDEWAYS\",\n");
        prompt.append("        \"trend_strength\": \"STRONG|MODERATE|WEAK\"\n");
        prompt.append("      },\n");
        prompt.append("      \"technical_indicators\": {\n");
        prompt.append("        \"rsi\": {\"value\": \"65.2\", \"signal\": \"NEUTRAL\"},\n");
        prompt.append("        \"macd\": {\"value\": \"0.0045\", \"signal\": \"BULLISH\"},\n");
        prompt.append("        \"moving_averages\": {\n");
        prompt.append("          \"ma20\": \"$90,500\",\n");
        prompt.append("          \"ma50\": \"$88,000\",\n");
        prompt.append("          \"alignment\": \"BULLISH|BEARISH|MIXED\"\n");
        prompt.append("        }\n");
        prompt.append("      },\n");
        prompt.append("      \"support_resistance\": {\n");
        prompt.append("        \"immediate_support\": \"$89,000\",\n");
        prompt.append("        \"strong_support\": \"$85,000\",\n");
        prompt.append("        \"immediate_resistance\": \"$95,000\",\n");
        prompt.append("        \"strong_resistance\": \"$100,000\"\n");
        prompt.append("      },\n");
        prompt.append("      \"chart_patterns\": {\n");
        prompt.append("        \"current_pattern\": \"TRIANGLE|FLAG|NONE\",\n");
        prompt.append("        \"pattern_confidence\": \"HIGH|MODERATE|LOW\"\n");
        prompt.append("      },\n");
        prompt.append("      \"technical_signals\": {\n");
        prompt.append("        \"primary_signal\": \"BUY|SELL|HOLD\",\n");
        prompt.append("        \"signal_strength\": \"STRONG|MODERATE|WEAK\"\n");
        prompt.append("      },\n");
        prompt.append("      \"trading_levels\": {\n");
        prompt.append("        \"entry_zone\": \"$90,000 - $91,000\",\n");
        prompt.append("        \"stop_loss\": \"$87,000\",\n");
        prompt.append("        \"take_profit_1\": \"$96,000\"\n");
        prompt.append("      },\n");
        prompt.append("      \"momentum_analysis\": {\n");
        prompt.append("        \"momentum_direction\": \"BULLISH|BEARISH|NEUTRAL\",\n");
        prompt.append("        \"momentum_strength\": \"STRONG|MODERATE|WEAK\",\n");
        prompt.append("        \"divergences\": \"POSITIVE|NEGATIVE|NONE\"\n");
        prompt.append("      },\n");
        prompt.append("      \"technical_advice\": {\n");
        prompt.append("        \"recommendation\": \"ACCUMULATE|REDUCE|HOLD|AVOID\",\n");
        prompt.append("        \"reasoning\": \"Brief technical reasoning\",\n");
        prompt.append("        \"timeframe\": \"1-3 days|1-2 weeks|1 month+\",\n");
        prompt.append("        \"confidence_level\": \"HIGH|MEDIUM|LOW\"\n");
        prompt.append("      }\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"technical_summary\": {\n");
        prompt.append("    \"strongest_technical_setups\": [\"BTC breakout\", \"ETH accumulation\"],\n");
        prompt.append("    \"weakest_technical_setups\": [\"ADA breakdown\"],\n");
        prompt.append("    \"key_levels_to_watch\": {\n");
        prompt.append("      \"critical_support\": [\"BTC $85K\", \"ETH $2.8K\"],\n");
        prompt.append("      \"critical_resistance\": [\"BTC $100K\", \"ETH $3.5K\"]\n");
        prompt.append("    },\n");
        prompt.append("    \"technical_outlook\": {\n");
        prompt.append("      \"short_term\": \"1-7 days outlook\",\n");
        prompt.append("      \"medium_term\": \"1-4 weeks outlook\"\n");
        prompt.append("      \"key_catalysts\": \"Upcoming technical events\"\n");
        prompt.append("    }\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append("IMPORTANT: Analyze ALL cryptocurrencies in the portfolio. Keep explanations technical but concise.\n");

        return prompt.toString();
    }

    /**
     * Parse AI response from technical analysis into structured data.
     */
    public Map<String, Object> parseTechnicalAnalysisResponse(String response) {
        Map<String, Object> parsed = new HashMap<>();

        try {
            // Strip code fences if present
            String clean = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

            // Extract the JSON block
            int start = clean.indexOf("{");
            int end = clean.lastIndexOf("}");
            String json = (start >= 0 && end > start) ? clean.substring(start, end + 1) : clean;

            JsonNode root = objectMapper.readTree(json);

            // Parse market technical overview
            if (root.has("market_technical_overview")) {
                Map<String, String> marketOverview = new HashMap<>();
                JsonNode overview = root.get("market_technical_overview");
                marketOverview.put("overall_market_trend", overview.has("overall_market_trend") ? overview.get("overall_market_trend").asText() : "");
                marketOverview.put("market_momentum", overview.has("market_momentum") ? overview.get("market_momentum").asText() : "");
                marketOverview.put("volatility_assessment", overview.has("volatility_assessment") ? overview.get("volatility_assessment").asText() : "");
                marketOverview.put("sector_rotation", overview.has("sector_rotation") ? overview.get("sector_rotation").asText() : "");
                marketOverview.put("key_market_levels", overview.has("key_market_levels") ? overview.get("key_market_levels").asText() : "");
                parsed.put("market_technical_overview", marketOverview);
            }

            // Parse technical analysis table
            if (root.has("technical_analysis_table") && root.get("technical_analysis_table").isArray()) {
                List<Map<String, Object>> analysisTable = new ArrayList<>();
                for (JsonNode crypto : root.get("technical_analysis_table")) {
                    Map<String, Object> cryptoAnalysis = new HashMap<>();
                    cryptoAnalysis.put("symbol", crypto.has("symbol") ? crypto.get("symbol").asText() : "");
                    cryptoAnalysis.put("current_price", crypto.has("current_price") ? crypto.get("current_price").asText() : "");

                    // Parse trend analysis
                    if (crypto.has("trend_analysis")) {
                        Map<String, String> trendAnalysis = new HashMap<>();
                        JsonNode trend = crypto.get("trend_analysis");
                        trendAnalysis.put("primary_trend", trend.has("primary_trend") ? trend.get("primary_trend").asText() : "");
                        trendAnalysis.put("trend_strength", trend.has("trend_strength") ? trend.get("trend_strength").asText() : "");
                        cryptoAnalysis.put("trend_analysis", trendAnalysis);
                    }

                    // Parse technical indicators
                    if (crypto.has("technical_indicators")) {
                        Map<String, Object> indicators = new HashMap<>();
                        JsonNode techIndicators = crypto.get("technical_indicators");

                        // RSI
                        if (techIndicators.has("rsi")) {
                            Map<String, String> rsi = new HashMap<>();
                            JsonNode rsiNode = techIndicators.get("rsi");
                            rsi.put("value", rsiNode.has("value") ? rsiNode.get("value").asText() : "");
                            rsi.put("signal", rsiNode.has("signal") ? rsiNode.get("signal").asText() : "");
                            indicators.put("rsi", rsi);
                        }

                        // MACD
                        if (techIndicators.has("macd")) {
                            Map<String, String> macd = new HashMap<>();
                            JsonNode macdNode = techIndicators.get("macd");
                            macd.put("value", macdNode.has("value") ? macdNode.get("value").asText() : "");
                            macd.put("signal", macdNode.has("signal") ? macdNode.get("signal").asText() : "");
                            indicators.put("macd", macd);
                        }

                        // Moving Averages
                        if (techIndicators.has("moving_averages")) {
                            Map<String, String> ma = new HashMap<>();
                            JsonNode maNode = techIndicators.get("moving_averages");
                            ma.put("ma20", maNode.has("ma20") ? maNode.get("ma20").asText() : "");
                            ma.put("ma50", maNode.has("ma50") ? maNode.get("ma50").asText() : "");
                            ma.put("alignment", maNode.has("alignment") ? maNode.get("alignment").asText() : "");
                            indicators.put("moving_averages", ma);
                        }

                        cryptoAnalysis.put("technical_indicators", indicators);
                    }

                    // Parse support/resistance levels
                    if (crypto.has("support_resistance")) {
                        Map<String, String> levels = new HashMap<>();
                        JsonNode sr = crypto.get("support_resistance");
                        levels.put("immediate_support", sr.has("immediate_support") ? sr.get("immediate_support").asText() : "");
                        levels.put("strong_support", sr.has("strong_support") ? sr.get("strong_support").asText() : "");
                        levels.put("immediate_resistance", sr.has("immediate_resistance") ? sr.get("immediate_resistance").asText() : "");
                        levels.put("strong_resistance", sr.has("strong_resistance") ? sr.get("strong_resistance").asText() : "");
                        cryptoAnalysis.put("support_resistance", levels);
                    }

                    // Parse chart patterns
                    if (crypto.has("chart_patterns")) {
                        Map<String, String> patterns = new HashMap<>();
                        JsonNode cp = crypto.get("chart_patterns");
                        patterns.put("current_pattern", cp.has("current_pattern") ? cp.get("current_pattern").asText() : "");
                        patterns.put("pattern_confidence", cp.has("pattern_confidence") ? cp.get("pattern_confidence").asText() : "");
                        cryptoAnalysis.put("chart_patterns", patterns);
                    }

                    // Parse technical signals
                    if (crypto.has("technical_signals")) {
                        Map<String, String> signals = new HashMap<>();
                        JsonNode ts = crypto.get("technical_signals");
                        signals.put("primary_signal", ts.has("primary_signal") ? ts.get("primary_signal").asText() : "");
                        signals.put("signal_strength", ts.has("signal_strength") ? ts.get("signal_strength").asText() : "");
                        cryptoAnalysis.put("technical_signals", signals);
                    }

                    // Parse trading levels
                    if (crypto.has("trading_levels")) {
                        Map<String, String> trading = new HashMap<>();
                        JsonNode tl = crypto.get("trading_levels");
                        trading.put("entry_zone", tl.has("entry_zone") ? tl.get("entry_zone").asText() : "");
                        trading.put("stop_loss", tl.has("stop_loss") ? tl.get("stop_loss").asText() : "");
                        trading.put("take_profit_1", tl.has("take_profit_1") ? tl.get("take_profit_1").asText() : "");
                        cryptoAnalysis.put("trading_levels", trading);
                    }

                    if (crypto.has("momentum_analysis")) {
                        Map<String, String> advice = new HashMap<>();
                        JsonNode ta = crypto.get("momentum_analysis");
                        advice.put("momentum_direction", ta.has("momentum_direction") ? ta.get("momentum_direction").asText() : "");
                        advice.put("momentum_strength", ta.has("momentum_strength") ? ta.get("momentum_strength").asText() : "");
                        advice.put("divergences", ta.has("divergences") ? ta.get("divergences").asText() : "");
                        cryptoAnalysis.put("momentum_analysis", advice);
                    }
                    // Parse technical advice
                    if (crypto.has("technical_advice")) {
                        Map<String, String> advice = new HashMap<>();
                        JsonNode ta = crypto.get("technical_advice");
                        advice.put("recommendation", ta.has("recommendation") ? ta.get("recommendation").asText() : "");
                        advice.put("reasoning", ta.has("reasoning") ? ta.get("reasoning").asText() : "");
                        advice.put("timeframe", ta.has("timeframe") ? ta.get("timeframe").asText() : "");
                        advice.put("confidence_level", ta.has("confidence_level") ? ta.get("confidence_level").asText() : "");
                        cryptoAnalysis.put("technical_advice", advice);
                    }

                    analysisTable.add(cryptoAnalysis);
                }
                parsed.put("technical_analysis_table", analysisTable);
            }

            // Parse technical summary
            if (root.has("technical_summary")) {
                Map<String, Object> summary = new HashMap<>();
                JsonNode summaryNode = root.get("technical_summary");

                // Parse strongest technical setups
                if (summaryNode.has("strongest_technical_setups") && summaryNode.get("strongest_technical_setups").isArray()) {
                    List<String> strongest = new ArrayList<>();
                    for (JsonNode setup : summaryNode.get("strongest_technical_setups")) {
                        strongest.add(setup.asText());
                    }
                    summary.put("strongest_technical_setups", strongest);
                } else {
                    summary.put("strongest_technical_setups", new ArrayList<>());
                }

                // Parse weakest technical setups
                if (summaryNode.has("weakest_technical_setups") && summaryNode.get("weakest_technical_setups").isArray()) {
                    List<String> weakest = new ArrayList<>();
                    for (JsonNode setup : summaryNode.get("weakest_technical_setups")) {
                        weakest.add(setup.asText());
                    }
                    summary.put("weakest_technical_setups", weakest);
                } else {
                    summary.put("weakest_technical_setups", new ArrayList<>());
                }

                // Parse key levels to watch
                if (summaryNode.has("key_levels_to_watch")) {
                    Map<String, List<String>> keyLevels = new HashMap<>();
                    JsonNode levels = summaryNode.get("key_levels_to_watch");

                    if (levels.has("critical_support") && levels.get("critical_support").isArray()) {
                        List<String> supports = new ArrayList<>();
                        for (JsonNode support : levels.get("critical_support")) {
                            supports.add(support.asText());
                        }
                        keyLevels.put("critical_support", supports);
                    } else {
                        keyLevels.put("critical_support", new ArrayList<>());
                    }

                    if (levels.has("critical_resistance") && levels.get("critical_resistance").isArray()) {
                        List<String> resistance = new ArrayList<>();
                        for (JsonNode res : levels.get("critical_resistance")) {
                            resistance.add(res.asText());
                        }
                        keyLevels.put("critical_resistance", resistance);
                    } else {
                        keyLevels.put("critical_resistance", new ArrayList<>());
                    }

                    summary.put("key_levels_to_watch", keyLevels);
                }

                // Parse technical outlook
                if (summaryNode.has("technical_outlook")) {
                    Map<String, String> outlook = new HashMap<>();
                    JsonNode outlookNode = summaryNode.get("technical_outlook");
                    outlook.put("short_term", outlookNode.has("short_term") ? outlookNode.get("short_term").asText() : "");
                    outlook.put("medium_term", outlookNode.has("medium_term") ? outlookNode.get("medium_term").asText() : "");
                    outlook.put("key_catalysts", outlookNode.has("key_catalysts") ? outlookNode.get("key_catalysts").asText() : "");
                    summary.put("technical_outlook", outlook);
                }

                parsed.put("technical_summary", summary);
            }

        } catch (JsonParseException e) {
            System.err.println("JSON Parse Error in TechnicalAnalysisService: " + e.getMessage());
            System.err.println("Error at line: " + e.getLocation().getLineNr() + ", column: " + e.getLocation().getColumnNr());
            System.err.println("Problematic JSON section around error:");
            try {
                String[] lines = response.split("\n");
                int errorLine = (int) e.getLocation().getLineNr();
                int start = Math.max(0, errorLine - 3);
                int end = Math.min(lines.length, errorLine + 3);
                for (int i = start; i < end; i++) {
                    String marker = (i + 1 == errorLine) ? " >>> " : "     ";
                    System.err.println(marker + (i + 1) + ": " + lines[i]);
                }
            } catch (Exception logEx) {
                System.err.println("Could not show JSON error context: " + logEx.getMessage());
            }

            // Safe defaults on failure
            addDefaultStructure(parsed);
        } catch (Exception e) {
            System.err.println("Error parsing technical analysis response: " + e.getMessage());
            e.printStackTrace();

            // Safe defaults on failure
            addDefaultStructure(parsed);
        }

        return parsed;
    }

    private void addDefaultStructure(Map<String, Object> parsed) {
        parsed.put("market_technical_overview", new HashMap<>());
        parsed.put("technical_analysis_table", new ArrayList<>());
        parsed.put("technical_summary", new HashMap<>());
    }
}
