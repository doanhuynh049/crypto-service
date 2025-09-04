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

    @Autowired
    private InvestmentStrategyService investmentStrategyService;

    @Autowired
    private RiskOpportunityAnalysisService riskOpportunityAnalysisService;
    
    @Autowired
    private OpportunityFinderAnalysisService opportunityFinderAnalysisService;
    
    @Autowired
    private EntryExitStrategyAnalysisService entryExitStrategyAnalysisService;
    
    @Autowired
    private PortfolioOptimizationAnalysisService portfolioOptimizationAnalysisService;
    
    @Autowired
    private PortfolioHealthCheckAnalysisService portfolioHealthCheckAnalysisService;

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
        String prompt = riskOpportunityAnalysisService.buildRiskOpportunityPrompt(holdings);
        System.out.println("Risk & Opportunity Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Risk & Opportunity Analysis AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = riskOpportunityAnalysisService.parseRiskOpportunityResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generatePortfolioHealthCheck(List<Holding> holdings) {
        String prompt = portfolioHealthCheckAnalysisService.buildPortfolioHealthCheckPrompt(holdings);
        System.out.println("Portfolio Health Check Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Portfolio Health Check AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = portfolioHealthCheckAnalysisService.parsePortfolioHealthCheckResponse(aiResponse);
        return parsedAnalysis;
    }
    

    public Map<String, Object> generateOpportunityFinder(List<Holding> holdings) {
        String prompt = opportunityFinderAnalysisService.buildOpportunityFinderPrompt(holdings);
        System.out.println("Opportunity Finder Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Opportunity Finder AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = opportunityFinderAnalysisService.parseOpportunityFinderResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generatePortfolioOptimizationAnalysis(List<Holding> holdings) {
        String prompt = portfolioOptimizationAnalysisService.buildPortfolioOptimizationPrompt(holdings);
        System.out.println("Portfolio Optimization Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Portfolio Optimization Analysis AI Response: " + aiResponse);
        
        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = portfolioOptimizationAnalysisService.parsePortfolioOptimizationResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generateEntryExitStrategy(List<Holding> holdings) {
        String prompt = entryExitStrategyAnalysisService.buildEntryExitStrategyPrompt(holdings);
        System.out.println("Entry & Exit Strategy Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("Entry & Exit Strategy Analysis AI Response: " + aiResponse);

        // Parse the AI response and return structured data using dedicated service
        Map<String, Object> parsedAnalysis = entryExitStrategyAnalysisService.parseEntryExitStrategyResponse(aiResponse);
        return parsedAnalysis;
    }

    public Map<String, Object> generateUSDTAllocationStrategy(List<Holding> holdings) {
        double usdtAmount = holdings.stream()
                .filter(h -> h.getSymbol().equalsIgnoreCase("USDT"))
                .findFirst()
                .map(Holding::getHoldings)
                .orElse(0.0);

        String prompt = buildUSDTAllocationPrompt(holdings, usdtAmount);
        System.out.println("USDT Allocation Strategy Analysis Prompt: " + prompt);
        String aiResponse = callGeminiAPI(prompt);
        System.out.println("USDT Allocation Strategy AI Response: " + aiResponse);

        // Parse the AI response and return structured data
        Map<String, Object> parsedAnalysis = parseUSDTAllocationResponse(aiResponse);
        return parsedAnalysis;
    }

    //=============================================================================================

    private String buildPromptForOverview(List<Holding> holdings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a crypto investment advisor. Analyze the following portfolio and provide structured advice.\n\n");
        prompt.append(investmentStrategyService.getPortfolioTargetsSection());
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
        prompt.append(investmentStrategyService.getInvestmentContextSection());

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

    public Map<String, Object> generatePortfolioTable(List<Holding> holdings) {
        Map<String, Object> portfolioData = new HashMap<>();
        List<Map<String, Object>> portfolioRows = new ArrayList<>();

        double totalInitialValue = 0;
        double totalCurrentValue = 0;
        double totalProfitLoss = 0;

        try {
            for (Holding holding : holdings) {
                Map<String, Object> row = new HashMap<>();

                // Get current market data
                MarketData marketData = dataProviderService.getMarketData(holding.getId());
                double currentPrice = marketData != null ? marketData.getCurrentPrice() : 0;

                // Basic holding information
                row.put("symbol", holding.getSymbol());
                row.put("name", holding.getName());
                row.put("holdings", holding.getHoldings());
                row.put("averagePrice", holding.getAveragePrice());
                row.put("currentPrice", currentPrice);

                // Target prices
                row.put("expectedEntry", holding.getExpectedEntry());
                row.put("deepEntryPrice", holding.getDeepEntryPrice());
                row.put("targetPrice3Month", holding.getTargetPrice3Month());
                row.put("targetPriceLongTerm", holding.getTargetPriceLongTerm());

                // Financial calculations
                double initialValue = holding.getInitialValue();
                double currentValue = holding.getHoldings() * currentPrice;
                double profitLoss = currentValue - initialValue;
                double profitLossPercentage = initialValue > 0 ? (profitLoss / initialValue) * 100 : 0;

                row.put("initialValue", initialValue);
                row.put("currentValue", currentValue);
                row.put("profitLoss", profitLoss);
                row.put("profitLossPercentage", profitLossPercentage);

                // Distance to targets
                double distanceTo3MonthTarget = currentPrice > 0 ?
                        ((holding.getTargetPrice3Month() - currentPrice) / currentPrice) * 100 : 0;
                double distanceToLongTarget = currentPrice > 0 ?
                        ((holding.getTargetPriceLongTerm() - currentPrice) / currentPrice) * 100 : 0;

                row.put("distanceTo3MonthTarget", distanceTo3MonthTarget);
                row.put("distanceToLongTarget", distanceToLongTarget);

                // Market data and technical indicators
                if (marketData != null) {
                    row.put("priceChange24h", marketData.getPriceChangePercentage24h());
                    row.put("volume24h", marketData.getVolume24h());
                    row.put("marketCap", marketData.getMarketCap());
                    row.put("rsi", marketData.getRsi());
                    row.put("macd", marketData.getMacd());
                    row.put("sma20", marketData.getSma20());
                    row.put("sma50", marketData.getSma50());
                    row.put("sma200", marketData.getSma200());

                    // Trend analysis
                    String trend = analyzeTrend(currentPrice, marketData);
                    row.put("trend", trend);

                    // Support and resistance levels
                    Map<String, Double> levels = calculateSupportResistanceLevels(marketData);
                    row.put("supportLevel", levels.get("support"));
                    row.put("resistanceLevel", levels.get("resistance"));
                } else {
                    // Default values when market data is not available
                    row.put("priceChange24h", 0.0);
                    row.put("volume24h", 0.0);
                    row.put("marketCap", 0.0);
                    row.put("trend", "UNKNOWN");
                    row.put("supportLevel", 0.0);
                    row.put("resistanceLevel", 0.0);
                }

                // Portfolio weight
                double portfolioWeight = totalInitialValue > 0 ? (initialValue / getTotalPortfolioValue(holdings)) * 100 : 0;
                row.put("portfolioWeight", portfolioWeight);

                // Risk assessment
                String riskLevel = assessRiskLevel(holding, marketData);
                row.put("riskLevel", riskLevel);

                // Action recommendation
                String recommendation = getActionRecommendation(holding, marketData, profitLossPercentage);
                row.put("recommendation", recommendation);

                portfolioRows.add(row);

                // Accumulate totals
                totalInitialValue += initialValue;
                totalCurrentValue += currentValue;
                totalProfitLoss += profitLoss;
            }

            // Portfolio summary
            double totalProfitLossPercentage = totalInitialValue > 0 ? (totalProfitLoss / totalInitialValue) * 100 : 0;

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalInitialValue", totalInitialValue);
            summary.put("totalCurrentValue", totalCurrentValue);
            summary.put("totalProfitLoss", totalProfitLoss);
            summary.put("totalProfitLossPercentage", totalProfitLossPercentage);
            summary.put("numberOfHoldings", holdings.size());

            // Risk distribution
            Map<String, Integer> riskDistribution = calculateRiskDistribution(portfolioRows);
            summary.put("riskDistribution", riskDistribution);

            portfolioData.put("portfolioRows", portfolioRows);
            portfolioData.put("summary", summary);
            portfolioData.put("timestamp", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            logger.error("Error generating portfolio table", e);
            portfolioData.put("error", "Failed to generate portfolio table: " + e.getMessage());
        }

        return portfolioData;
    }


    private Map<String, Integer> calculateRiskDistribution(List<Map<String, Object>> portfolioRows) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("LOW", 0);
        distribution.put("MEDIUM", 0);
        distribution.put("HIGH", 0);
        distribution.put("UNKNOWN", 0);

        for (Map<String, Object> row : portfolioRows) {
            String risk = (String) row.get("riskLevel");
            distribution.put(risk, distribution.get(risk) + 1);
        }

        return distribution;
    }



    private String analyzeTrend(double currentPrice, MarketData marketData) {
        try {
            if (marketData.getSma20() != null && marketData.getSma50() != null) {
                if (currentPrice > marketData.getSma20() && marketData.getSma20() > marketData.getSma50()) {
                    return "BULLISH";
                } else if (currentPrice < marketData.getSma20() && marketData.getSma20() < marketData.getSma50()) {
                    return "BEARISH";
                } else {
                    return "SIDEWAYS";
                }
            }

            // Fallback to 24h change
            if (marketData.getPriceChangePercentage24h() > 5) {
                return "BULLISH";
            } else if (marketData.getPriceChangePercentage24h() < -5) {
                return "BEARISH";
            } else {
                return "SIDEWAYS";
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private Map<String, Double> calculateSupportResistanceLevels(MarketData marketData) {
        Map<String, Double> levels = new HashMap<>();
        try {
            double currentPrice = marketData.getCurrentPrice();

            // Simple support/resistance calculation based on SMAs
            if (marketData.getSma20() != null && marketData.getSma50() != null) {
                double support = Math.min(marketData.getSma20(), marketData.getSma50());
                double resistance = Math.max(marketData.getSma20(), marketData.getSma50());

                // Adjust based on current price position
                if (currentPrice < support) {
                    resistance = support;
                    support = currentPrice * 0.95; // 5% below current
                } else if (currentPrice > resistance) {
                    support = resistance;
                    resistance = currentPrice * 1.05; // 5% above current
                }

                levels.put("support", support);
                levels.put("resistance", resistance);
            } else {
                // Fallback calculation
                levels.put("support", currentPrice * 0.95);
                levels.put("resistance", currentPrice * 1.05);
            }
        } catch (Exception e) {
            levels.put("support", 0.0);
            levels.put("resistance", 0.0);
        }

        return levels;
    }

    private String assessRiskLevel(Holding holding, MarketData marketData) {
        try {
            // Risk assessment based on volatility, RSI, and position size
            int riskScore = 0;

            // RSI-based risk
            if (marketData != null && marketData.getRsi() != null) {
                if (marketData.getRsi() > 80) riskScore += 2; // Overbought
                else if (marketData.getRsi() > 70) riskScore += 1;
                else if (marketData.getRsi() < 20) riskScore += 2; // Oversold
                else if (marketData.getRsi() < 30) riskScore += 1;
            }

            // 24h change based risk
            if (marketData != null) {
                double change = Math.abs(marketData.getPriceChangePercentage24h());
                if (change > 20) riskScore += 3;
                else if (change > 10) riskScore += 2;
                else if (change > 5) riskScore += 1;
            }

            // Position value risk (higher value = higher risk)
            double positionValue = holding.getInitialValue();
            if (positionValue > 50000) riskScore += 2;
            else if (positionValue > 20000) riskScore += 1;

            if (riskScore >= 5) return "HIGH";
            else if (riskScore >= 3) return "MEDIUM";
            else return "LOW";

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String getActionRecommendation(Holding holding, MarketData marketData, double profitLossPercentage) {
        try {
            // Simple recommendation logic
            if (profitLossPercentage > 50) {
                return "TAKE_PARTIAL_PROFIT";
            } else if (profitLossPercentage > 20) {
                return "HOLD_STRONG";
            } else if (profitLossPercentage < -20) {
                return "CONSIDER_AVERAGING_DOWN";
            } else if (marketData != null && marketData.getRsi() != null) {
                if (marketData.getRsi() > 80) {
                    return "WAIT_FOR_DIP";
                } else if (marketData.getRsi() < 30) {
                    return "CONSIDER_BUYING";
                }
            }
            return "HOLD";
        } catch (Exception e) {
            return "REVIEW";
        }
    }
    private double getTotalPortfolioValue(List<Holding> holdings) {
        return holdings.stream()
                .mapToDouble(Holding::getInitialValue)
                .sum();
    }
    private String buildUSDTAllocationPrompt(List<Holding> holdings, double usdtAmount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("USDT Allocation Strategy Analysis Prompt\n");
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

    private Map<String, Object> parseUSDTAllocationResponse(String response) {
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
