package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.config.AppConfig;
import com.quat.cryptoNotifier.model.Advisory;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.MarketData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdvisoryEngineService {

    @Autowired
    private AppConfig appConfig;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AdvisoryEngineService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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

    private String buildPrompt(Holding holding, MarketData marketData, Advisory advisory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a crypto investment advisor. Analyze the following position and provide structured advice.\n\n");
        
        prompt.append("POSITION DETAILS:\n");
        prompt.append(String.format("Symbol: %s\n", holding.getSymbol()));
        prompt.append(String.format("Amount: %.4f\n", holding.getAmount()));
        prompt.append(String.format("Average Price: $%.2f\n", holding.getAveragePrice()));
        prompt.append(String.format("Target Price: $%.2f\n", holding.getTargetPrice()));
        prompt.append(String.format("Max Drawdown: %.1f%%\n", holding.getMaxDrawdownPercentage()));
        
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
}
