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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AdvisoryEngineService {

    private static final Logger logger = LoggerFactory.getLogger(AdvisoryEngineService.class);
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private DataProviderService dataProviderService;

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
    

    public Map<String, Object> generateOpportunityFinder(List<Holding> holdings) {
        String prompt = buildOpportunityFinderPrompt(holdings);
        System.out.println("Opportunity Finder Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Opportunity Finder AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parseOpportunityFinderResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generatePortfolioOptimizationAnalysis(List<Holding> holdings) {
        String prompt = buildPortfolioOptimizationPrompt(holdings);
        System.out.println("Portfolio Optimization Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Portfolio Optimization Analysis AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parsePortfolioOptimizationResponse(aiResponse);
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

    private String buildOpportunityFinderPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Opportunity Finder Prompt\n");
        prompt.append("Analyze my crypto portfolio and identify:\n");
        prompt.append("- Which coins have the strongest long-term fundamentals.\n");
        prompt.append("- Which ones are speculative or risky.\n");
        prompt.append("- Where I could add new assets to improve diversification.\n");
        prompt.append("- Which coins I should reduce or exit.\n\n");
        
        prompt.append("Portfolio data:\n\n");
        
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
            prompt.append(String.format("Expected Entry Price: $%.2f\n", holding.getExpectedEntry()));
            prompt.append(String.format("Expected Target Price: $%.2f\n", holding.getExpectedPrice()));
            prompt.append(String.format("3-Month Target: $%.2f\n", holding.getTargetPrice3Month()));
            prompt.append(String.format("Long-term Target: $%.2f\n", holding.getTargetPriceLongTerm()));
            
            // Calculate potential returns for context
            double targetReturn3M = ((holding.getTargetPrice3Month() - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
            double targetReturnLong = ((holding.getTargetPriceLongTerm() - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
            prompt.append(String.format("Expected 3M Return: %.1f%%\n", targetReturn3M));
            prompt.append(String.format("Expected Long Return: %.1f%%\n", targetReturnLong));
            prompt.append("\n");
        }
        
        // Add analysis framework
        prompt.append("--- Analysis Framework ---\n");
        prompt.append("For fundamental strength assessment:\n");
        prompt.append("- Evaluate technology innovation, adoption metrics, partnerships\n");
        prompt.append("- Consider development activity, community strength, institutional backing\n");
        prompt.append("- Assess competitive positioning and sustainable competitive advantages\n");
        prompt.append("- Review tokenomics, governance, and long-term value proposition\n\n");
        
        prompt.append("For risk assessment:\n");
        prompt.append("- Identify speculative elements (hype-driven, unproven use cases)\n");
        prompt.append("- Evaluate regulatory risks, technical risks, market risks\n");
        prompt.append("- Consider concentration risk within portfolio\n");
        prompt.append("- Assess volatility patterns and correlation risks\n\n");
        
        prompt.append("For diversification opportunities:\n");
        prompt.append("- Identify missing sectors (AI, DeFi, Gaming, Infrastructure, etc.)\n");
        prompt.append("- Consider geographic diversification (different blockchain ecosystems)\n");
        prompt.append("- Evaluate market cap diversification (large-cap vs mid-cap opportunities)\n");
        prompt.append("- Suggest defensive assets or yield-generating opportunities\n\n");
        
        prompt.append("For exit/reduce recommendations:\n");
        prompt.append("- Identify assets with deteriorating fundamentals\n");
        prompt.append("- Consider overvalued positions or profit-taking opportunities\n");
        prompt.append("- Evaluate portfolio concentration risks\n");
        prompt.append("- Assess assets that no longer fit investment thesis\n\n");
        
        prompt.append("--- Investment Context ---\n");
        prompt.append("Investment timeframe: 6 months – 3 years\n");
        prompt.append("Risk tolerance: Moderate\n");
        prompt.append("Portfolio size: $" + String.format("%.0f", totalPortfolioValue) + "\n");
        prompt.append("Focus: Strong fundamentals, real-world utility, sustainable long-term value\n");
        prompt.append("Exclusions: No meme coins or highly speculative low-cap tokens\n");
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
        prompt.append("        \"current_allocation\": \"0%\",\n");
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

    private Map<String, Object> parseOpportunityFinderResponse(String response) {
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
                JsonNode strengthNode = responseNode.get("fundamental_strength_analysis");
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
                        coinData.put("recommendation", coin.has("recommendation") ? coin.get("recommendation").asText() : "MONITOR");
                        coinData.put("rationale", coin.has("rationale") ? coin.get("rationale").asText() : "");
                        moderate.add(coinData);
                    }
                    strengthAnalysis.put("moderate_coins", moderate);
                }
                
                parsedData.put("fundamental_strength_analysis", strengthAnalysis);
            }
            
            // Parse risk assessment
            if (responseNode.has("risk_assessment")) {
                JsonNode riskNode = responseNode.get("risk_assessment");
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
                
                riskAssessment.put("portfolio_risks", riskNode.has("portfolio_risks") ? riskNode.get("portfolio_risks").asText() : "");
                riskAssessment.put("concentration_concerns", riskNode.has("concentration_concerns") ? riskNode.get("concentration_concerns").asText() : "");
                parsedData.put("risk_assessment", riskAssessment);
            }
            
            // Parse diversification opportunities
            if (responseNode.has("diversification_opportunities")) {
                JsonNode diversificationNode = responseNode.get("diversification_opportunities");
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
                parsedData.put("diversification_opportunities", diversification);
            }
            
            // Parse exit/reduce recommendations
            if (responseNode.has("exit_reduce_recommendations")) {
                JsonNode exitNode = responseNode.get("exit_reduce_recommendations");
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
                
                parsedData.put("exit_reduce_recommendations", exitRecommendations);
            }
            
            // Parse action plan
            if (responseNode.has("action_plan")) {
                JsonNode actionNode = responseNode.get("action_plan");
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
                parsedData.put("action_plan", actionPlan);
            }
            
            // Parse market timing considerations
            parsedData.put("market_timing_considerations", responseNode.has("market_timing_considerations") ? responseNode.get("market_timing_considerations").asText() : "");
            
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

    private String buildPortfolioOptimizationPrompt(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("🔍 Portfolio Optimization Analysis Prompt\n");
        prompt.append("**Please provide insights on the following:**\n\n");
        
        prompt.append("1. **Overall risk profile** of the portfolio — consider asset class exposure (Layer 1s, DeFi, etc.), volatility, and diversification.\n");
        prompt.append("2. How well is the portfolio **balanced between upside potential and downside risk**?\n");
        prompt.append("3. Which assets appear **overweighted or underweighted** based on their fundamentals, growth outlook, and risk levels?\n");
        prompt.append("4. **Actionable suggestions to rebalance** the portfolio for stronger long-term opportunity capture (e.g., adding exposure to AI, DeFi, Web3 infrastructure).\n");
        prompt.append("5. Are there any **key sectors or assets missing** that I should consider for better diversification? (e.g., stablecoins, staking coins, interoperability tokens, or blue-chip infrastructure projects).\n\n");
        
        prompt.append("⚠️ **Context & Preferences:**\n");
        prompt.append("- Investment timeframe: **6 months to 3 years**\n");
        prompt.append("- Risk tolerance: **Moderate**\n");
        prompt.append("- I prefer projects with **strong fundamentals**, **real-world utility**, and **sustainable long-term value**\n");
        prompt.append("- Please **exclude meme coins** or highly speculative low-cap tokens from any recommendations\n\n");
        
        prompt.append("I'm seeking a thoughtful, data-driven analysis to optimize my portfolio for both growth and risk management.\n\n");
        
        prompt.append("Portfolio data:\n\n");
        
        // Calculate total portfolio value and individual percentages
        double totalPortfolioValue = 0;
        for (Holding holding : holdings) {
            totalPortfolioValue += holding.getInitialValue();
        }
        
        // Add detailed portfolio information with current weightings
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

    private Map<String, Object> parsePortfolioOptimizationResponse(String response) {
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

    // Investment Analysis Methods
    public Map<String, Object> generateInvestmentAnalysis(Holding holding) {
        try {
            // Fetch market data for the specified crypto using DataProviderService
            MarketData marketData = dataProviderService.getMarketData(holding.getId());
            
            // Convert MarketData to Map for compatibility with prompt builder
            Map<String, Object> marketDataMap = convertMarketDataToMap(marketData);
            
            // Build prompt for comprehensive investment analysis
            String prompt = buildInvestmentAnalysisPrompt(holding, marketDataMap);
            
            // Get AI analysis using Gemini API
            String aiResponse = callGeminiAPI(prompt);
            
            // Parse and structure the response
            return parseInvestmentAnalysisResponse(aiResponse, holding.getSymbol());
            
        } catch (Exception e) {
            logger.error("Error generating investment analysis for {}: ", holding.getSymbol(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate investment analysis for " + holding.getSymbol());
            errorResponse.put("bottom_line", "WAIT - Unable to analyze current market conditions");
            errorResponse.put("recommendation", "WAIT");
            errorResponse.put("confidence", "LOW");
            errorResponse.put("symbol", holding.getSymbol());
            errorResponse.put("name", holding.getName());
            return errorResponse;
        }
    }
    
    // Legacy method for ETH analysis - calls the generic method
    public Map<String, Object> generateETHInvestmentAnalysis() {
        Holding ethHolding = new Holding();
        ethHolding.setSymbol("ETH");
        ethHolding.setName("Ethereum");
        return generateInvestmentAnalysis(ethHolding);
    }
    
    private Map<String, Object> convertMarketDataToMap(MarketData marketData) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("price", marketData.getCurrentPrice());
        dataMap.put("priceChange24h", marketData.getPriceChangePercentage24h());
        dataMap.put("priceChange7d", 0.0); // Not available in MarketData, set default
        dataMap.put("marketCap", 0.0); // Not available in MarketData, set default
        dataMap.put("volume24h", marketData.getVolume24h());
        return dataMap;
    }

    private String buildInvestmentAnalysisPrompt(Holding holding, Map<String, Object> marketData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a professional crypto investment analyst providing a comprehensive ").append(holding.getSymbol()).append(" investment analysis. ");
        prompt.append("Analyze the current ").append(holding.getSymbol()).append(" market conditions and provide a detailed, data-backed evaluation following this EXACT structure:\n\n");
        
        prompt.append("Current ").append(holding.getSymbol()).append(" Data:\n");
        prompt.append("- Price: $").append(marketData.get("price")).append("\n");
        prompt.append("- 24h Change: ").append(marketData.get("priceChange24h")).append("%\n");
        prompt.append("- 7d Change: ").append(marketData.get("priceChange7d")).append("%\n");
        prompt.append("- Market Cap: $").append(marketData.get("marketCap")).append("\n");
        prompt.append("- Volume 24h: $").append(marketData.get("volume24h")).append("\n\n");
        
        // Include holding-specific context if available
        if (holding.getAveragePrice() > 0) {
            prompt.append("Current Position Context:\n");
            prompt.append("- Your average buy price: $").append(holding.getAveragePrice()).append("\n");
            prompt.append("- Holdings: ").append(holding.getHoldings()).append(" ").append(holding.getSymbol()).append("\n");
            if (holding.getTargetPrice3Month() > 0) {
                prompt.append("- Your 3M target: $").append(holding.getTargetPrice3Month()).append("\n");
            }
            if (holding.getTargetPriceLongTerm() > 0) {
                prompt.append("- Your long-term target: $").append(holding.getTargetPriceLongTerm()).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("Provide your analysis in this EXACT JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"bottom_line\": \"BUY/WAIT/DCA - One clear sentence with reasoning\",\n");
        prompt.append("  \"recommendation\": \"BUY/WAIT/DCA\",\n");
        prompt.append("  \"confidence\": \"HIGH/MEDIUM/LOW\",\n");
        prompt.append("  \"target_allocation\": \"Recommended portfolio percentage for ").append(holding.getSymbol()).append("\",\n");
        prompt.append("  \"current_price_analysis\": {\n");
        prompt.append("    \"entry_quality\": \"EXCELLENT/GOOD/FAIR/POOR\",\n");
        prompt.append("    \"price_context\": \"Analysis of current price relative to recent ranges\",\n");
        prompt.append("    \"value_assessment\": \"Whether ").append(holding.getSymbol()).append(" is undervalued, fairly valued, or overvalued\",\n");
        prompt.append("    \"timing_factors\": \"Key factors affecting entry timing\"\n");
        prompt.append("  },\n");
        prompt.append("  \"technical_levels\": {\n");
        prompt.append("    \"support_levels\": [\"$2800\", \"$2650\", \"$2400\"],\n");
        prompt.append("    \"resistance_levels\": [\"$3200\", \"$3400\", \"$3600\"],\n");
        prompt.append("    \"key_breakout_level\": \"Critical level for bullish momentum\",\n");
        prompt.append("    \"stop_loss_suggestion\": \"Recommended stop-loss level\"\n");
        prompt.append("  },\n");
        prompt.append("  \"outlook\": {\n");
        prompt.append("    \"short_term\": \"1-3 month outlook with key factors\",\n");
        prompt.append("    \"medium_term\": \"3-12 month outlook and expectations\",\n");
        prompt.append("    \"long_term\": \"1-3 year outlook and potential\"\n");
        prompt.append("  },\n");
        prompt.append("  \"strategy\": {\n");
        prompt.append("    \"entry_strategy\": \"Recommended approach for entering position\",\n");
        prompt.append("    \"dca_schedule\": \"If DCA recommended, suggested schedule\",\n");
        prompt.append("    \"profit_targets\": [\"First target\", \"Second target\", \"Long-term target\"],\n");
        prompt.append("    \"risk_management\": \"Key risk management considerations\"\n");
        prompt.append("  },\n");
        prompt.append("  \"market_sentiment\": {\n");
        prompt.append("    \"current_sentiment\": \"BULLISH/NEUTRAL/BEARISH\",\n");
        prompt.append("    \"institutional_activity\": \"Analysis of institutional interest\",\n");
        prompt.append("    \"retail_sentiment\": \"Retail investor sentiment analysis\",\n");
        prompt.append("    \"macro_factors\": \"Key macroeconomic factors affecting ").append(holding.getSymbol()).append("\"\n");
        prompt.append("  },\n");
        prompt.append("  \"fundamentals\": {\n");
        prompt.append("    \"network_health\": \"").append(holding.getName()).append(" network metrics and health\",\n");
        prompt.append("    \"staking_metrics\": \"").append(holding.getSymbol()).append(" staking data and implications (if applicable)\",\n");
        prompt.append("    \"ecosystem_activity\": \"Ecosystem activity and ").append(holding.getSymbol()).append(" demand\",\n");
        prompt.append("    \"development_activity\": \"").append(holding.getName()).append(" development progress\",\n");
        prompt.append("    \"competitive_position\": \"").append(holding.getSymbol()).append(" vs competitors in its sector\"\n");
        prompt.append("  },\n");
        prompt.append("  \"catalysts_and_risks\": {\n");
        prompt.append("    \"positive_catalysts\": [\"Bullish factor 1\", \"Bullish factor 2\", \"Bullish factor 3\"],\n");
        prompt.append("    \"risk_factors\": [\"Risk factor 1\", \"Risk factor 2\", \"Risk factor 3\"],\n");
        prompt.append("    \"upcoming_events\": [\"Important upcoming events affecting ").append(holding.getSymbol()).append("\"],\n");
        prompt.append("    \"regulatory_outlook\": \"Regulatory environment analysis\"\n");
        prompt.append("  },\n");
        prompt.append("  \"position_sizing\": {\n");
        prompt.append("    \"conservative_allocation\": \"Recommended % for conservative investors\",\n");
        prompt.append("    \"moderate_allocation\": \"Recommended % for moderate risk investors\",\n");
        prompt.append("    \"aggressive_allocation\": \"Recommended % for aggressive investors\",\n");
        prompt.append("    \"sizing_rationale\": \"Reasoning behind allocation recommendations\"\n");
        prompt.append("  },\n");
        prompt.append("  \"key_triggers\": {\n");
        prompt.append("    \"buy_triggers\": [\"Conditions that would trigger buy signal\"],\n");
        prompt.append("    \"sell_triggers\": [\"Conditions that would trigger sell signal\"],\n");
        prompt.append("    \"reassessment_triggers\": [\"Events that would require strategy reassessment\"]\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("Ensure your analysis is:\n");
        prompt.append("- Data-driven and objective\n");
        prompt.append("- Considers both technical and fundamental factors\n");
        prompt.append("- Includes specific price levels and percentages where applicable\n");
        prompt.append("- Provides actionable recommendations\n");
        prompt.append("- Acknowledges risks and uncertainties\n");
        prompt.append("- Suitable for different risk tolerance levels\n");
        prompt.append("- Tailored specifically to ").append(holding.getSymbol()).append(" and its unique characteristics");
        
        return prompt.toString();
    }

    private Map<String, Object> parseInvestmentAnalysisResponse(String response, String symbol) {
        Map<String, Object> parsedData = new HashMap<>();
        
        try {
            // Clean the response
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            
            // Find JSON content
            String jsonResponse = cleanResponse;
            int jsonStart = cleanResponse.indexOf("{");
            int jsonEnd = cleanResponse.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse = cleanResponse.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode responseNode = objectMapper.readTree(jsonResponse);
            
            // Parse top-level fields
            parsedData.put("symbol", symbol);
            parsedData.put("bottom_line", responseNode.has("bottom_line") ? responseNode.get("bottom_line").asText() : "WAIT - Analysis incomplete");
            parsedData.put("recommendation", responseNode.has("recommendation") ? responseNode.get("recommendation").asText() : "WAIT");
            parsedData.put("confidence", responseNode.has("confidence") ? responseNode.get("confidence").asText() : "LOW");
            parsedData.put("target_allocation", responseNode.has("target_allocation") ? responseNode.get("target_allocation").asText() : "5-10%");
            
            // Parse current price analysis
            if (responseNode.has("current_price_analysis")) {
                JsonNode priceNode = responseNode.get("current_price_analysis");
                Map<String, String> priceAnalysis = new HashMap<>();
                priceAnalysis.put("entry_quality", priceNode.has("entry_quality") ? priceNode.get("entry_quality").asText() : "FAIR");
                priceAnalysis.put("price_context", priceNode.has("price_context") ? priceNode.get("price_context").asText() : "Current price analysis pending");
                priceAnalysis.put("value_assessment", priceNode.has("value_assessment") ? priceNode.get("value_assessment").asText() : "Fairly valued");
                priceAnalysis.put("timing_factors", priceNode.has("timing_factors") ? priceNode.get("timing_factors").asText() : "Market timing considerations");
                parsedData.put("current_price_analysis", priceAnalysis);
            }
            
            // Parse technical levels
            if (responseNode.has("technical_levels")) {
                JsonNode techNode = responseNode.get("technical_levels");
                Map<String, Object> technicalLevels = new HashMap<>();
                
                // Parse support levels
                if (techNode.has("support_levels")) {
                    List<String> supportLevels = new ArrayList<>();
                    JsonNode supportArray = techNode.get("support_levels");
                    for (JsonNode level : supportArray) {
                        supportLevels.add(level.asText());
                    }
                    technicalLevels.put("support_levels", supportLevels);
                }
                
                // Parse resistance levels
                if (techNode.has("resistance_levels")) {
                    List<String> resistanceLevels = new ArrayList<>();
                    JsonNode resistanceArray = techNode.get("resistance_levels");
                    for (JsonNode level : resistanceArray) {
                        resistanceLevels.add(level.asText());
                    }
                    technicalLevels.put("resistance_levels", resistanceLevels);
                }
                
                technicalLevels.put("key_breakout_level", techNode.has("key_breakout_level") ? techNode.get("key_breakout_level").asText() : "TBD");
                technicalLevels.put("stop_loss_suggestion", techNode.has("stop_loss_suggestion") ? techNode.get("stop_loss_suggestion").asText() : "Set based on risk tolerance");
                parsedData.put("technical_levels", technicalLevels);
            }
            
            // Parse outlook
            if (responseNode.has("outlook")) {
                JsonNode outlookNode = responseNode.get("outlook");
                Map<String, String> outlook = new HashMap<>();
                outlook.put("short_term", outlookNode.has("short_term") ? outlookNode.get("short_term").asText() : "Short-term outlook pending");
                outlook.put("medium_term", outlookNode.has("medium_term") ? outlookNode.get("medium_term").asText() : "Medium-term outlook pending");
                outlook.put("long_term", outlookNode.has("long_term") ? outlookNode.get("long_term").asText() : "Long-term outlook pending");
                parsedData.put("outlook", outlook);
            }
            
            // Parse strategy
            if (responseNode.has("strategy")) {
                JsonNode strategyNode = responseNode.get("strategy");
                Map<String, Object> strategy = new HashMap<>();
                strategy.put("entry_strategy", strategyNode.has("entry_strategy") ? strategyNode.get("entry_strategy").asText() : "Gradual entry recommended");
                strategy.put("dca_schedule", strategyNode.has("dca_schedule") ? strategyNode.get("dca_schedule").asText() : "Weekly or bi-weekly");
                strategy.put("risk_management", strategyNode.has("risk_management") ? strategyNode.get("risk_management").asText() : "Standard risk management principles");
                
                // Parse profit targets
                if (strategyNode.has("profit_targets")) {
                    List<String> profitTargets = new ArrayList<>();
                    JsonNode targetsArray = strategyNode.get("profit_targets");
                    for (JsonNode target : targetsArray) {
                        profitTargets.add(target.asText());
                    }
                    strategy.put("profit_targets", profitTargets);
                }
                
                parsedData.put("strategy", strategy);
            }
            
            // Parse market sentiment
            if (responseNode.has("market_sentiment")) {
                JsonNode sentimentNode = responseNode.get("market_sentiment");
                Map<String, String> marketSentiment = new HashMap<>();
                marketSentiment.put("current_sentiment", sentimentNode.has("current_sentiment") ? sentimentNode.get("current_sentiment").asText() : "NEUTRAL");
                marketSentiment.put("institutional_activity", sentimentNode.has("institutional_activity") ? sentimentNode.get("institutional_activity").asText() : "Institutional activity analysis");
                marketSentiment.put("retail_sentiment", sentimentNode.has("retail_sentiment") ? sentimentNode.get("retail_sentiment").asText() : "Retail sentiment analysis");
                marketSentiment.put("macro_factors", sentimentNode.has("macro_factors") ? sentimentNode.get("macro_factors").asText() : "Macro factors analysis");
                parsedData.put("market_sentiment", marketSentiment);
            }
            
            // Parse fundamentals
            if (responseNode.has("fundamentals")) {
                JsonNode fundamentalsNode = responseNode.get("fundamentals");
                Map<String, String> fundamentals = new HashMap<>();
                fundamentals.put("network_health", fundamentalsNode.has("network_health") ? fundamentalsNode.get("network_health").asText() : "Network health analysis");
                fundamentals.put("staking_metrics", fundamentalsNode.has("staking_metrics") ? fundamentalsNode.get("staking_metrics").asText() : "Staking metrics analysis");
                fundamentals.put("ecosystem_activity", fundamentalsNode.has("ecosystem_activity") ? fundamentalsNode.get("ecosystem_activity").asText() : "Ecosystem activity analysis");
                fundamentals.put("development_activity", fundamentalsNode.has("development_activity") ? fundamentalsNode.get("development_activity").asText() : "Development activity analysis");
                fundamentals.put("competitive_position", fundamentalsNode.has("competitive_position") ? fundamentalsNode.get("competitive_position").asText() : "Competitive position analysis");
                parsedData.put("fundamentals", fundamentals);
            }
            
            // Parse catalysts and risks
            if (responseNode.has("catalysts_and_risks")) {
                JsonNode catalystsNode = responseNode.get("catalysts_and_risks");
                Map<String, Object> catalystsRisks = new HashMap<>();
                
                // Parse positive catalysts
                if (catalystsNode.has("positive_catalysts")) {
                    List<String> positiveCatalysts = new ArrayList<>();
                    JsonNode catalystsArray = catalystsNode.get("positive_catalysts");
                    for (JsonNode catalyst : catalystsArray) {
                        positiveCatalysts.add(catalyst.asText());
                    }
                    catalystsRisks.put("positive_catalysts", positiveCatalysts);
                }
                
                // Parse risk factors
                if (catalystsNode.has("risk_factors")) {
                    List<String> riskFactors = new ArrayList<>();
                    JsonNode risksArray = catalystsNode.get("risk_factors");
                    for (JsonNode risk : risksArray) {
                        riskFactors.add(risk.asText());
                    }
                    catalystsRisks.put("risk_factors", riskFactors);
                }
                
                // Parse upcoming events
                if (catalystsNode.has("upcoming_events")) {
                    List<String> upcomingEvents = new ArrayList<>();
                    JsonNode eventsArray = catalystsNode.get("upcoming_events");
                    for (JsonNode event : eventsArray) {
                        upcomingEvents.add(event.asText());
                    }
                    catalystsRisks.put("upcoming_events", upcomingEvents);
                }
                
                catalystsRisks.put("regulatory_outlook", catalystsNode.has("regulatory_outlook") ? catalystsNode.get("regulatory_outlook").asText() : "Regulatory outlook analysis");
                parsedData.put("catalysts_and_risks", catalystsRisks);
            }
            
            // Parse position sizing
            if (responseNode.has("position_sizing")) {
                JsonNode positionNode = responseNode.get("position_sizing");
                Map<String, String> positionSizing = new HashMap<>();
                positionSizing.put("conservative_allocation", positionNode.has("conservative_allocation") ? positionNode.get("conservative_allocation").asText() : "5-10%");
                positionSizing.put("moderate_allocation", positionNode.has("moderate_allocation") ? positionNode.get("moderate_allocation").asText() : "10-20%");
                positionSizing.put("aggressive_allocation", positionNode.has("aggressive_allocation") ? positionNode.get("aggressive_allocation").asText() : "20-30%");
                positionSizing.put("sizing_rationale", positionNode.has("sizing_rationale") ? positionNode.get("sizing_rationale").asText() : "Allocation rationale");
                parsedData.put("position_sizing", positionSizing);
            }
            
            // Parse key triggers
            if (responseNode.has("key_triggers")) {
                JsonNode triggersNode = responseNode.get("key_triggers");
                Map<String, Object> keyTriggers = new HashMap<>();
                
                // Parse buy triggers
                if (triggersNode.has("buy_triggers")) {
                    List<String> buyTriggers = new ArrayList<>();
                    JsonNode buyArray = triggersNode.get("buy_triggers");
                    for (JsonNode trigger : buyArray) {
                        buyTriggers.add(trigger.asText());
                    }
                    keyTriggers.put("buy_triggers", buyTriggers);
                }
                
                // Parse sell triggers
                if (triggersNode.has("sell_triggers")) {
                    List<String> sellTriggers = new ArrayList<>();
                    JsonNode sellArray = triggersNode.get("sell_triggers");
                    for (JsonNode trigger : sellArray) {
                        sellTriggers.add(trigger.asText());
                    }
                    keyTriggers.put("sell_triggers", sellTriggers);
                }
                
                // Parse reassessment triggers
                if (triggersNode.has("reassessment_triggers")) {
                    List<String> reassessmentTriggers = new ArrayList<>();
                    JsonNode reassessmentArray = triggersNode.get("reassessment_triggers");
                    for (JsonNode trigger : reassessmentArray) {
                        reassessmentTriggers.add(trigger.asText());
                    }
                    keyTriggers.put("reassessment_triggers", reassessmentTriggers);
                }
                
                parsedData.put("key_triggers", keyTriggers);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing investment analysis response for {}: ", symbol, e);
            // Return default structured response
            parsedData.put("symbol", symbol);
            parsedData.put("bottom_line", "WAIT - Unable to parse analysis response");
            parsedData.put("recommendation", "WAIT");
            parsedData.put("confidence", "LOW");
            parsedData.put("current_price_analysis", new HashMap<>());
            parsedData.put("technical_levels", new HashMap<>());
            parsedData.put("outlook", new HashMap<>());
            parsedData.put("strategy", new HashMap<>());
            parsedData.put("market_sentiment", new HashMap<>());
            parsedData.put("fundamentals", new HashMap<>());
            parsedData.put("catalysts_and_risks", new HashMap<>());
            parsedData.put("position_sizing", new HashMap<>());
            parsedData.put("key_triggers", new HashMap<>());
        }
        
        return parsedData;
    }
}
