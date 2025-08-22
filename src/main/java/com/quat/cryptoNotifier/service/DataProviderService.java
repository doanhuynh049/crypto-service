package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.MarketData;
import com.quat.cryptoNotifier.util.IndicatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataProviderService {

    @Autowired
    private MarketDataCacheService cacheService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DataProviderService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Public method that uses caching - this is what other services should call
     */
    public MarketData getMarketData(String coinGeckoId) {
        return cacheService.getMarketData(coinGeckoId);
    }

    /**
     * Direct API call method used by cache service - bypasses cache
     */
    public MarketData fetchMarketDataDirect(String coinGeckoId) {
        try {
            // Get current price from CoinGecko
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

            MarketData marketData = new MarketData(coinGeckoId, coinData.get("usd").asDouble());
            
            if (coinData.has("usd_24h_change")) {
                marketData.setPriceChangePercentage24h(coinData.get("usd_24h_change").asDouble());
            }
            
            if (coinData.has("usd_24h_vol")) {
                marketData.setVolume24h(coinData.get("usd_24h_vol").asDouble());
            }
            Thread.sleep(30000); // To respect API rate limits
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
            Thread.sleep(30000); // To respect API rate limits

            marketData.setPrices(historicalPrices);

            // Calculate technical indicators
            calculateTechnicalIndicators(marketData, historicalPrices);

            return marketData;

        } catch (Exception e) {
            System.err.println("[DataProviderService] Error fetching market data for " + coinGeckoId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch market data for " + coinGeckoId, e);
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
}
