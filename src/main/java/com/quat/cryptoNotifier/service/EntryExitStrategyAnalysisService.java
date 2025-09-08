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
     * Build a daily trading recommendations prompt focused on actionable buy/sell decisions.
     * 
     * This method creates a prompt that asks the AI to analyze current holdings against
     * their target prices and market trends to provide specific buy/sell recommendations
     * with exact price levels for immediate action TODAY.
     */
    public String buildEntryExitStrategyPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== DAILY CRYPTO TRADING RECOMMENDATIONS ===\n");
        prompt.append("⚠️ CRITICAL: KEEP TOTAL RESPONSE UNDER 3000 WORDS. BE CONCISE AND ACTIONABLE.\n");
        prompt.append("This analysis is based on my overall investment strategy and current portfolio holdings.\n\n");
        prompt.append(investmentStrategyService.getCompleteInvestmentStrategySection());
        prompt.append("Generate ACTIONABLE buy/sell recommendations for TODAY (immediate execution) based on my target prices and current market conditions.\n\n");

        prompt.append("REQUIREMENTS (Keep responses brief):\n");
        prompt.append("- ⚡ TODAY ONLY: Only trades for immediate execution\n");
        prompt.append("- 🎯 CONFIDENCE: HIGH/MEDIUM/LOW with brief reasoning\n");
        prompt.append("- 📊 PRIORITY: HIGH/MEDIUM/LOW ranking\n");
        prompt.append("- 💰 TAKE-PROFIT: 1-2 levels max with % gains\n");
        prompt.append("- 🛡️ RISK CHECKS: Portfolio limits compliance\n");
        prompt.append("- ⏰ URGENCY: Specific timeframes (2-4h, 4-6h, today)\n");
        prompt.append("- 🔄 STOP-LOSS: Mandatory levels for all trades\n\n");

        prompt.append("MY PORTFOLIO TARGETS & CURRENT DATA:\n\n");

        // Add detailed portfolio information with target prices for comparison
        for (Holding holding : holdings) {
            prompt.append(String.format("--- %s (%s) ---\n", holding.getSymbol(), holding.getName()));
            prompt.append(String.format("Current Holdings: %.6f %s\n", holding.getHoldings(), holding.getSymbol()));
            prompt.append(String.format("Average Buy Price: $%.2f\n", holding.getAveragePrice()));
            
            // Add target prices for comparison
            prompt.append(String.format("Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("Deep Entry Price: $%.2f\n", holding.getDeepEntryPrice()));
            prompt.append(String.format("3-Month Price Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("Long-term Price Target: $%.2f\n", holding.getTargetPriceLongTerm()));

            // Fetch and include current market data for comparison
            try {
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                if (marketData != null) {
                    double currentPrice = marketData.getCurrentPrice();
                    prompt.append(String.format("Current Price: $%.2f\n", currentPrice));
                    prompt.append(String.format("24h Change: %.2f%%\n", marketData.getPriceChangePercentage24h()));
                    
                    // Calculate price vs targets
                    double vs3MonthTarget = ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100;
                    double vsLongTarget = ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100;
                    double vsExpectedEntry = ((currentPrice - holding.getExpectedEntry()) / holding.getExpectedEntry()) * 100;
                    double vsDeepEntry = ((currentPrice - holding.getDeepEntryPrice()) / holding.getDeepEntryPrice()) * 100;
                    
                    prompt.append(String.format("Distance to 3M Target: %.1f%% (%.0f%% upside potential)\n", vs3MonthTarget, vs3MonthTarget));
                    prompt.append(String.format("Distance to Long Target: %.1f%% (%.0f%% upside potential)\n", vsLongTarget, vsLongTarget));
                    prompt.append(String.format("vs Expected Entry: %.1f%% (%s entry level)\n", vsExpectedEntry, 
                        vsExpectedEntry < -5 ? "GOOD" : vsExpectedEntry > 10 ? "EXPENSIVE" : "FAIR"));
                    prompt.append(String.format("vs Deep Entry: %.1f%% (%s entry level)\n", vsDeepEntry, 
                        vsDeepEntry < -5 ? "EXCELLENT" : vsDeepEntry > 15 ? "VERY EXPENSIVE" : "ACCEPTABLE"));

                    // Current P&L for context
                    double currentValue = holding.getHoldings() * currentPrice;
                    double profitLoss = currentValue - holding.getTotalAvgCost();
                    double profitLossPercentage = (profitLoss / holding.getTotalAvgCost()) * 100;
                    prompt.append(String.format("Current P&L: $%.2f (%.1f%%)\n", profitLoss, profitLossPercentage));

                    // Technical indicators for momentum
                    if (marketData.getRsi() != null) {
                        prompt.append(String.format("RSI: %.1f (%s)\n", marketData.getRsi(), 
                            marketData.getRsi() < 30 ? "OVERSOLD" : marketData.getRsi() > 70 ? "OVERBOUGHT" : "NEUTRAL"));
                    }
                }
            } catch (Exception e) {
                prompt.append("--- Market Data Unavailable ---\n");
                System.err.println("Could not fetch market data for " + holding.getSymbol() + ": " + e.getMessage());
            }

            prompt.append("\n");
        }

        // Add centralized investment strategy context
        prompt.append("--- Context & Preferences ---\n");
        prompt.append(investmentStrategyService.getInvestmentContextSection());

        // Add CONCISE output format specification for daily recommendations
        prompt.append("--- CONCISE JSON Output Format (KEEP RESPONSES SHORT) ---\n");
        prompt.append("Provide BRIEF analysis in this JSON format. Keep each field under 50 words:\n");
        prompt.append("{\n");
        prompt.append("  \"market_overview\": {\n");
        prompt.append("    \"market_sentiment\": \"BULLISH|BEARISH|NEUTRAL\",\n");
        prompt.append("    \"key_levels\": \"Brief key support/resistance levels\",\n");
        prompt.append("    \"best_trading_window\": \"Optimal trading time today\"\n");
        prompt.append("  },\n");
        prompt.append("  \"sell_recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"BTC\",\n");
        prompt.append("      \"action\": \"SELL_NOW|TRIM_POSITION\",\n");
        prompt.append("      \"confidence_level\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("      \"priority\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("      \"sell_amount_percentage\": \"25%\",\n");
        prompt.append("      \"target_sell_price\": \"$95,000\",\n");
        prompt.append("      \"current_price_vs_target\": \"Current: $92K, Target: $95K (+3.3%)\",\n");
        prompt.append("      \"reason\": \"Brief reason (max 30 words)\",\n");
        prompt.append("      \"urgency\": \"2-4 hours|4-6 hours|today\",\n");
        prompt.append("      \"take_profit_levels\": [\n");
        prompt.append("        {\"price\": \"$95,000\", \"gain\": \"15%\", \"sell_amount\": \"33%\"}\n");
        prompt.append("      ],\n");
        prompt.append("      \"stop_loss_level\": \"$88,000\",\n");
        prompt.append("      \"limit_order_price\": \"$94,500\",\n");
        prompt.append("      \"risk_reward_ratio\": \"1:2.5\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"buy_recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"ETH\",\n");
        prompt.append("      \"action\": \"BUY_NOW|DCA_TODAY\",\n");
        prompt.append("      \"confidence_level\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("      \"priority\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("      \"buy_amount_usd\": \"$500-1000\",\n");
        prompt.append("      \"target_buy_price\": \"$2,800\",\n");
        prompt.append("      \"current_price_vs_target\": \"Current: $2,850, Target: $2,800 (-1.8%)\",\n");
        prompt.append("      \"reason\": \"Brief reason (max 30 words)\",\n");
        prompt.append("      \"urgency\": \"2-4 hours|4-6 hours|today\",\n");
        prompt.append("      \"stop_loss_level\": \"$2,600\",\n");
        prompt.append("      \"take_profit_levels\": [\n");
        prompt.append("        {\"price\": \"$3,100\", \"gain\": \"11%\", \"sell_amount\": \"25%\"}\n");
        prompt.append("      ],\n");
        prompt.append("      \"limit_order_price\": \"$2,820\",\n");
        prompt.append("      \"risk_reward_ratio\": \"1:2.0\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"hold_recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"symbol\": \"SOL\",\n");
        prompt.append("      \"reason\": \"Brief reason to hold (max 30 words)\",\n");
        prompt.append("      \"watch_levels\": {\n");
        prompt.append("        \"buy_below\": \"$180\",\n");
        prompt.append("        \"sell_above\": \"$220\"\n");
        prompt.append("      }\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"portfolio_actions\": {\n");
        prompt.append("    \"rebalancing_needed\": \"Yes/No with brief action\",\n");
        prompt.append("    \"priority_order\": [\"Brief action 1\", \"Brief action 2\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"daily_execution_plan\": {\n");
        prompt.append("    \"morning_actions\": [\"Set BTC sell limit $94.5K\", \"Set ETH buy limit $2.82K\"],\n");
        prompt.append("    \"alerts_to_configure\": [\"BTC $88K stop\", \"ETH $3.1K profit\"]\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append("IMPORTANT: Keep all text fields under 50 words. Be concise but actionable.\n");

        return prompt.toString();
    }

    /**
     * Parse AI response from daily trading recommendations into structured data.
     * 
     * This method processes the AI's JSON response and extracts all relevant information
     * including market overview, sell recommendations, buy recommendations, hold recommendations,
     * portfolio actions, and daily execution plan.
     */
    public Map<String, Object> parseEntryExitStrategyResponse(String response) {
        Map<String, Object> parsed = new HashMap<>();

        try {            
            // Strip code fences if present
            String clean = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

            // Extract the JSON block
            int start = clean.indexOf("{");
            int end = clean.lastIndexOf("}");
            String json = (start >= 0 && end > start) ? clean.substring(start, end + 1) : clean;
            
            JsonNode root = objectMapper.readTree(json);

            // Parse market overview (simplified)
            if (root.has("market_overview")) {
                Map<String, String> marketOverview = new HashMap<>();
                JsonNode overview = root.get("market_overview");
                marketOverview.put("market_sentiment", overview.has("market_sentiment") ? overview.get("market_sentiment").asText() : "");
                marketOverview.put("sentiment_confidence", overview.has("sentiment_confidence") ? overview.get("sentiment_confidence").asText() : "");
                marketOverview.put("key_levels", overview.has("key_levels") ? overview.get("key_levels").asText() : "");
                marketOverview.put("trend_alignment", overview.has("trend_alignment") ? overview.get("trend_alignment").asText() : "");
                marketOverview.put("volatility_expectation", overview.has("volatility_expectation") ? overview.get("volatility_expectation").asText() : "");
                marketOverview.put("best_trading_window", overview.has("best_trading_window") ? overview.get("best_trading_window").asText() : "");
                marketOverview.put("market_catalysts", overview.has("market_catalysts") ? overview.get("market_catalysts").asText() : "");
                marketOverview.put("overnight_alerts", overview.has("overnight_alerts") ? overview.get("overnight_alerts").asText() : "");
                parsed.put("market_overview", marketOverview);
            }

            // Parse sell recommendations (simplified)
            if (root.has("sell_recommendations") && root.get("sell_recommendations").isArray()) {
                List<Map<String, Object>> sellRecs = new ArrayList<>();
                for (JsonNode rec : root.get("sell_recommendations")) {
                    Map<String, Object> sellRec = new HashMap<>();
                    sellRec.put("symbol", rec.has("symbol") ? rec.get("symbol").asText() : "");
                    sellRec.put("action", rec.has("action") ? rec.get("action").asText() : "");
                    sellRec.put("confidence_level", rec.has("confidence_level") ? rec.get("confidence_level").asText() : "");
                    sellRec.put("priority", rec.has("priority") ? rec.get("priority").asText() : "");
                    sellRec.put("sell_amount_percentage", rec.has("sell_amount_percentage") ? rec.get("sell_amount_percentage").asText() : "");
                    sellRec.put("target_sell_price", rec.has("target_sell_price") ? rec.get("target_sell_price").asText() : "");
                    sellRec.put("current_price_vs_target", rec.has("current_price_vs_target") ? rec.get("current_price_vs_target").asText() : "N/A");
                    sellRec.put("reason", rec.has("reason") ? rec.get("reason").asText() : "");
                    sellRec.put("urgency", rec.has("urgency") ? rec.get("urgency").asText() : "");
                    sellRec.put("urgency_reasoning", rec.has("urgency_reasoning") ? rec.get("urgency_reasoning").asText() : "");
                    sellRec.put("confidence_reasoning", rec.has("confidence_reasoning") ? rec.get("confidence_reasoning").asText() : "");
                    sellRec.put("stop_loss_level", rec.has("stop_loss_level") ? rec.get("stop_loss_level").asText() : "");
                    sellRec.put("stop_loss_reasoning", rec.has("stop_loss_reasoning") ? rec.get("stop_loss_reasoning").asText() : "");
                    sellRec.put("limit_order_price", rec.has("limit_order_price") ? rec.get("limit_order_price").asText() : "");
                    sellRec.put("risk_reward_ratio", rec.has("risk_reward_ratio") ? rec.get("risk_reward_ratio").asText() : "");
                    sellRec.put("market_conditions_required", rec.has("market_conditions_required") ? rec.get("market_conditions_required").asText() : "");
                    
                    // Parse simplified take profit levels
                    if (rec.has("take_profit_levels") && rec.get("take_profit_levels").isArray()) {
                        List<Map<String, String>> takeProfits = new ArrayList<>();
                        for (JsonNode tp : rec.get("take_profit_levels")) {
                            Map<String, String> tpLevel = new HashMap<>();
                            tpLevel.put("price", tp.has("price") ? tp.get("price").asText() : "");
                            tpLevel.put("gain", tp.has("gain") ? tp.get("gain").asText() : "");
                            tpLevel.put("sell_amount", tp.has("sell_amount") ? tp.get("sell_amount").asText() : "");
                            tpLevel.put("probability", tp.has("probability") ? tp.get("probability").asText() : "");
                            takeProfits.add(tpLevel);
                        }
                        sellRec.put("take_profit_levels", takeProfits);
                    }
                    sellRecs.add(sellRec);
                }
                parsed.put("sell_recommendations", sellRecs);
            }

            // Parse buy recommendations (simplified)
            if (root.has("buy_recommendations") && root.get("buy_recommendations").isArray()) {
                List<Map<String, Object>> buyRecs = new ArrayList<>();
                for (JsonNode rec : root.get("buy_recommendations")) {
                    Map<String, Object> buyRec = new HashMap<>();
                    buyRec.put("symbol", rec.has("symbol") ? rec.get("symbol").asText() : "");
                    buyRec.put("action", rec.has("action") ? rec.get("action").asText() : "");
                    buyRec.put("confidence_level", rec.has("confidence_level") ? rec.get("confidence_level").asText() : "");
                    buyRec.put("priority", rec.has("priority") ? rec.get("priority").asText() : "");
                    buyRec.put("buy_amount_usd", rec.has("buy_amount_usd") ? rec.get("buy_amount_usd").asText() : "");
                    buyRec.put("target_buy_price", rec.has("target_buy_price") ? rec.get("target_buy_price").asText() : "");
                    buyRec.put("current_price_vs_target", rec.has("current_price_vs_target") ? rec.get("current_price_vs_target").asText() : "N/A");
                    buyRec.put("reason", rec.has("reason") ? rec.get("reason").asText() : "");
                    buyRec.put("urgency", rec.has("urgency") ? rec.get("urgency").asText() : "");
                    buyRec.put("urgency_reasoning", rec.has("urgency_reasoning") ? rec.get("urgency_reasoning").asText() : "");
                    buyRec.put("confidence_reasoning", rec.has("confidence_reasoning") ? rec.get("confidence_reasoning").asText() : "");
                    buyRec.put("stop_loss_level", rec.has("stop_loss_level") ? rec.get("stop_loss_level").asText() : "");
                    buyRec.put("stop_loss_reasoning", rec.has("stop_loss_reasoning") ? rec.get("stop_loss_reasoning").asText() : "");
                    buyRec.put("limit_order_price", rec.has("limit_order_price") ? rec.get("limit_order_price").asText() : "");
                    buyRec.put("risk_reward_ratio", rec.has("risk_reward_ratio") ? rec.get("risk_reward_ratio").asText() : "");
                    buyRec.put("dca_strategy", rec.has("dca_strategy") ? rec.get("dca_strategy").asText() : "");
                    buyRec.put("market_conditions_required", rec.has("market_conditions_required") ? rec.get("market_conditions_required").asText() : "");
                    
                    // Parse simplified take profit levels
                    if (rec.has("take_profit_levels") && rec.get("take_profit_levels").isArray()) {
                        List<Map<String, String>> takeProfits = new ArrayList<>();
                        for (JsonNode tp : rec.get("take_profit_levels")) {
                            Map<String, String> tpLevel = new HashMap<>();
                            tpLevel.put("price", tp.has("price") ? tp.get("price").asText() : "");
                            tpLevel.put("gain", tp.has("gain") ? tp.get("gain").asText() : "");
                            tpLevel.put("sell_amount", tp.has("sell_amount") ? tp.get("sell_amount").asText() : "");
                            tpLevel.put("probability", tp.has("probability") ? tp.get("probability").asText() : "");
                            takeProfits.add(tpLevel);
                        }
                        buyRec.put("take_profit_levels", takeProfits);
                    }
                    buyRecs.add(buyRec);
                }
                parsed.put("buy_recommendations", buyRecs);
            }

            // Parse hold recommendations (simplified)
            if (root.has("hold_recommendations") && root.get("hold_recommendations").isArray()) {
                List<Map<String, Object>> holdRecs = new ArrayList<>();
                for (JsonNode rec : root.get("hold_recommendations")) {
                    Map<String, Object> holdRec = new HashMap<>();
                    holdRec.put("symbol", rec.has("symbol") ? rec.get("symbol").asText() : "");
                    holdRec.put("reason", rec.has("reason") ? rec.get("reason").asText() : "");
                    holdRec.put("confidence_level", rec.has("confidence_level") ? rec.get("confidence_level").asText() : "");
                    holdRec.put("expected_move_today", rec.has("expected_move_today") ? rec.get("expected_move_today").asText() : "");
                    holdRec.put("catalyst_watch", rec.has("catalyst_watch") ? rec.get("catalyst_watch").asText() : "");
                    
                    // Parse watch levels
                    if (rec.has("watch_levels")) {
                        Map<String, String> watchLevels = new HashMap<>();
                        JsonNode levels = rec.get("watch_levels");
                        watchLevels.put("buy_below", levels.has("buy_below") ? levels.get("buy_below").asText() : "");
                        watchLevels.put("sell_above", levels.has("sell_above") ? levels.get("sell_above").asText() : "");
                        watchLevels.put("key_support", levels.has("key_support") ? levels.get("key_support").asText() : "");
                        watchLevels.put("key_resistance", levels.has("key_resistance") ? levels.get("key_resistance").asText() : "");
                        holdRec.put("watch_levels", watchLevels);
                    } else {
                        Map<String, String> watchLevels = new HashMap<>();
                        watchLevels.put("buy_below", "");
                        watchLevels.put("sell_above", "");
                        watchLevels.put("key_support", "");
                        watchLevels.put("key_resistance", "");
                        holdRec.put("watch_levels", watchLevels);
                    }
                    
                    // Parse alerts to set
                    if (rec.has("alerts_to_set") && rec.get("alerts_to_set").isArray()) {
                        List<String> alerts = new ArrayList<>();
                        for (JsonNode alert : rec.get("alerts_to_set")) {
                            alerts.add(alert.asText());
                        }
                        holdRec.put("alerts_to_set", alerts);
                    } else {
                        holdRec.put("alerts_to_set", new ArrayList<>());
                    }
                    holdRecs.add(holdRec);
                }
                parsed.put("hold_recommendations", holdRecs);
            }

            // Parse portfolio risk check
            if (root.has("portfolio_risk_check")) {
                Map<String, Object> portfolioRiskCheck = new HashMap<>();
                JsonNode riskCheck = root.get("portfolio_risk_check");
                portfolioRiskCheck.put("current_allocation_warnings", riskCheck.has("current_allocation_warnings") ? riskCheck.get("current_allocation_warnings").asText() : "");
                portfolioRiskCheck.put("stablecoin_buffer_status", riskCheck.has("stablecoin_buffer_status") ? riskCheck.get("stablecoin_buffer_status").asText() : "");
                portfolioRiskCheck.put("position_size_compliance", riskCheck.has("position_size_compliance") ? riskCheck.get("position_size_compliance").asText() : "");
                portfolioRiskCheck.put("post_trade_portfolio_health", riskCheck.has("post_trade_portfolio_health") ? riskCheck.get("post_trade_portfolio_health").asText() : "");
                portfolioRiskCheck.put("risk_score", riskCheck.has("risk_score") ? riskCheck.get("risk_score").asText() : "");
                portfolioRiskCheck.put("correlation_risk", riskCheck.has("correlation_risk") ? riskCheck.get("correlation_risk").asText() : "");
                portfolioRiskCheck.put("liquidity_buffer", riskCheck.has("liquidity_buffer") ? riskCheck.get("liquidity_buffer").asText() : "");
                parsed.put("portfolio_risk_check", portfolioRiskCheck);
            }

            // Parse portfolio actions
            if (root.has("portfolio_actions")) {
                Map<String, Object> portfolioActions = new HashMap<>();
                JsonNode actions = root.get("portfolio_actions");
                portfolioActions.put("rebalancing_needed", actions.has("rebalancing_needed") ? actions.get("rebalancing_needed").asText() : "");
                portfolioActions.put("cash_deployment", actions.has("cash_deployment") ? actions.get("cash_deployment").asText() : "");
                portfolioActions.put("risk_management", actions.has("risk_management") ? actions.get("risk_management").asText() : "");
                portfolioActions.put("total_capital_at_risk", actions.has("total_capital_at_risk") ? actions.get("total_capital_at_risk").asText() : "");
                portfolioActions.put("expected_portfolio_changes", actions.has("expected_portfolio_changes") ? actions.get("expected_portfolio_changes").asText() : "");
                
                if (actions.has("priority_order") && actions.get("priority_order").isArray()) {
                    List<String> priorities = new ArrayList<>();
                    for (JsonNode priority : actions.get("priority_order")) {
                        priorities.add(priority.asText());
                    }
                    portfolioActions.put("priority_order", priorities);
                } else {
                    portfolioActions.put("priority_order", new ArrayList<>());
                }
                parsed.put("portfolio_actions", portfolioActions);
            }

            // Parse daily execution plan
            if (root.has("daily_execution_plan")) {
                Map<String, List<String>> executionPlan = new HashMap<>();
                JsonNode plan = root.get("daily_execution_plan");
                
                // Parse all timeline sections
                String[] timelineSections = {
                    "pre_market_preparation",
                    "morning_actions", 
                    "afternoon_monitoring",
                    "evening_review",
                    "limit_orders_to_set",
                    "alerts_to_configure",
                    "overnight_monitoring",
                    "risk_checkpoints"
                };
                
                for (String section : timelineSections) {
                    if (plan.has(section) && plan.get(section).isArray()) {
                        List<String> sectionActions = new ArrayList<>();
                        for (JsonNode action : plan.get(section)) {
                            sectionActions.add(action.asText());
                        }
                        executionPlan.put(section, sectionActions);
                    } else {
                        executionPlan.put(section, new ArrayList<>());
                    }
                }
                
                parsed.put("daily_execution_plan", executionPlan);
            }

        } catch (JsonParseException e) {
            System.err.println("JSON Parse Error: " + e.getMessage());
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
            parsed.put("market_overview", new HashMap<>());
            parsed.put("sell_recommendations", new ArrayList<>());
            parsed.put("buy_recommendations", new ArrayList<>());
            parsed.put("hold_recommendations", new ArrayList<>());
            parsed.put("portfolio_risk_check", new HashMap<>());
            parsed.put("portfolio_actions", new HashMap<>());
            parsed.put("daily_execution_plan", new HashMap<>());
        } catch (Exception e) {
            System.err.println("Error parsing daily trading recommendations response: " + e.getMessage());
            e.printStackTrace();

            // Safe defaults on failure
            parsed.put("market_overview", new HashMap<>());
            parsed.put("sell_recommendations", new ArrayList<>());
            parsed.put("buy_recommendations", new ArrayList<>());
            parsed.put("hold_recommendations", new ArrayList<>());
            parsed.put("portfolio_risk_check", new HashMap<>());
            parsed.put("portfolio_actions", new HashMap<>());
            parsed.put("daily_execution_plan", new HashMap<>());
        }

        return parsed;
    }
}


