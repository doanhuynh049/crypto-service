package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.Holding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvestmentAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(InvestmentAnalysisService.class);
    private final ObjectMapper objectMapper;

    public InvestmentAnalysisService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Builds a comprehensive investment analysis prompt for a specific cryptocurrency holding
     * @param holding The cryptocurrency holding to analyze
     * @param marketData Market data in Map format containing price, volume, etc.
     * @return A formatted prompt string for AI analysis
     */
    public String buildInvestmentAnalysisPrompt(Holding holding, Map<String, Object> marketData) {
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

    /**
     * Parses the AI response for investment analysis and structures it into a comprehensive Map
     * @param response The raw AI response string
     * @param symbol The cryptocurrency symbol being analyzed
     * @return A structured Map containing all parsed analysis data
     */
    public Map<String, Object> parseInvestmentAnalysisResponse(String response, String symbol) {
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
