package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.MarketData;
import com.quat.cryptoNotifier.util.IndicatorUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataProviderService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DataProviderService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public MarketData getMarketData(String symbolOrId) {
        try {
            // Get current price from CoinGecko
            String coinGeckoId = getCoinGeckoId(symbolOrId);
            String priceUrl = String.format("https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=usd&include_24hr_change=true&include_24hr_vol=true", 
                coinGeckoId);
            System.out.println("[DataProviderService] Fetching price from: " + priceUrl);
            String priceResponse = restTemplate.getForObject(priceUrl, String.class);
            JsonNode priceNode = objectMapper.readTree(priceResponse);
            JsonNode coinData = priceNode.get(coinGeckoId);
            if (coinData == null) {
                System.err.println("[DataProviderService] No data found for " + coinGeckoId + " in price response: " + priceResponse);
                throw new RuntimeException("No data found for " + coinGeckoId);
            }

            MarketData marketData = new MarketData(symbolOrId, coinData.get("usd").asDouble());
            
            if (coinData.has("usd_24h_change")) {
                marketData.setPriceChangePercentage24h(coinData.get("usd_24h_change").asDouble());
            }
            
            if (coinData.has("usd_24h_vol")) {
                marketData.setVolume24h(coinData.get("usd_24h_vol").asDouble());
            }
            Thread.sleep(1000); // To respect API rate limits
            // Get historical data for technical indicators
            String historyUrl = String.format("https://api.coingecko.com/api/v3/coins/%s/market_chart?vs_currency=usd&days=200&interval=daily", 
                coinGeckoId);
            System.out.println("[DataProviderService] Fetching history from: " + historyUrl);
            String historyResponse = restTemplate.getForObject(historyUrl, String.class);
            JsonNode historyNode = objectMapper.readTree(historyResponse);
            JsonNode pricesArray = historyNode.get("prices");
            if (pricesArray == null) {
                System.err.println("[DataProviderService] No prices array found for " + coinGeckoId + " in history response: " + historyResponse);
                throw new RuntimeException("No prices array found for " + coinGeckoId);
            }

            List<Double> historicalPrices = new ArrayList<>();
            for (JsonNode pricePoint : pricesArray) {
                historicalPrices.add(pricePoint.get(1).asDouble());
            }

            marketData.setPrices(historicalPrices);

            // Calculate technical indicators
            calculateTechnicalIndicators(marketData, historicalPrices);

            return marketData;

        } catch (Exception e) {
            System.err.println("[DataProviderService] Error fetching market data for " + symbolOrId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch market data for " + symbolOrId, e);
        }
    }

    private void calculateTechnicalIndicators(MarketData marketData, List<Double> prices) {
        try {
            // Calculate RSI (14 period)
            Double rsi = IndicatorUtils.calculateRSI(prices, 14);
            marketData.setRsi(rsi);

            // Calculate SMAs
            Double sma20 = IndicatorUtils.calculateSMA(prices, 20);
            marketData.setSma20(sma20);

            Double sma50 = IndicatorUtils.calculateSMA(prices, 50);
            marketData.setSma50(sma50);

            Double sma200 = IndicatorUtils.calculateSMA(prices, 200);
            marketData.setSma200(sma200);

            // Calculate MACD
            IndicatorUtils.MACDResult macdResult = IndicatorUtils.calculateMACD(prices, 12, 26, 9);
            if (macdResult != null) {
                marketData.setMacd(macdResult.getMacd());
                marketData.setMacdSignal(macdResult.getSignal());
            }

        } catch (Exception e) {
            System.err.println("Error calculating technical indicators for " + marketData.getSymbol() + ": " + e.getMessage());
        }
    }

    private String getCoinGeckoId(String symbolOrId) {
        // If it's already a known CoinGecko ID, return it directly
        if (isKnownCoinGeckoId(symbolOrId)) {
            return symbolOrId;
        }
        
        // Map common symbols to CoinGecko IDs
        switch (symbolOrId.toUpperCase()) {
            case "BTC":
                return "bitcoin";
            case "ETH":
                return "ethereum";
            case "ADA":
                return "cardano";
            case "DOT":
                return "polkadot";
            case "SOL":
                return "solana";
            case "MATIC":
                return "matic-network";
            case "LINK":
                return "chainlink";
            case "UNI":
                return "uniswap";
            case "AVAX":
                return "avalanche-2";
            case "ATOM":
                return "cosmos";
            case "BNB":
                return "binancecoin";
            case "OP":
                return "optimism";
            case "ARB":
                return "arbitrum";
            case "SUI":
                return "sui";
            case "RNDR":
                return "render-token";
            case "FET":
                return "fetch-ai";
            case "C":
                return "chainbase";
            default:
                return symbolOrId.toLowerCase();
        }
    }

    private boolean isKnownCoinGeckoId(String input) {
        // Check if the input is already a known CoinGecko ID
        String[] knownIds = {
            "bitcoin", "ethereum", "cardano", "polkadot", "solana", 
            "matic-network", "chainlink", "uniswap", "avalanche-2", "cosmos",
            "binancecoin", "optimism", "arbitrum", "sui", "render-token", 
            "fetch-ai", "chainbase"
        };
        
        for (String id : knownIds) {
            if (id.equals(input.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
