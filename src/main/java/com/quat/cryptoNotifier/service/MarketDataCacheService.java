package com.quat.cryptoNotifier.service;

import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.Holdings;
import com.quat.cryptoNotifier.model.MarketData;
import com.quat.cryptoNotifier.util.IndicatorUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MarketDataCacheService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<String, CachedMarketData> cache = new ConcurrentHashMap<>();
    private final ReentrantLock cacheLock = new ReentrantLock();

    // Cache entry with timestamp
    private static class CachedMarketData {
        private final MarketData marketData;
        private final LocalDateTime cacheTime;

        public CachedMarketData(MarketData marketData) {
            this.marketData = marketData;
            this.cacheTime = LocalDateTime.now();
        }

        public MarketData getMarketData() {
            return marketData;
        }

        public LocalDateTime getCacheTime() {
            return cacheTime;
        }

        public boolean isExpired(int cacheHours) {
            return LocalDateTime.now().isAfter(cacheTime.plusHours(cacheHours));
        }
    }

    /**
     * Get market data with caching (12-hour cache duration)
     */
    public MarketData getMarketData(String coinGeckoId) {
        return getMarketData(coinGeckoId, 12);
    }

    /**
     * Get market data with custom cache duration
     */
    public MarketData getMarketData(String coinGeckoId, int cacheHours) {
        cacheLock.lock();
        try {
            CachedMarketData cachedData = cache.get(coinGeckoId);

            // Check if cache exists and is not expired
            if (cachedData != null && !cachedData.isExpired(cacheHours)) {
                System.out.println("[MarketDataCache] Using cached data for " + coinGeckoId +
                    " (cached at: " + cachedData.getCacheTime() + ")");
                return cachedData.getMarketData();
            }

            // Cache miss or expired - fetch fresh data
            System.out.println("[MarketDataCache] Cache miss/expired for " + coinGeckoId +
                " - fetching fresh data");

            MarketData freshData = fetchMarketDataDirect(coinGeckoId);
            cache.put(coinGeckoId, new CachedMarketData(freshData));

            System.out.println("[MarketDataCache] Cached fresh data for " + coinGeckoId);
            return freshData;

        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Refresh cache for a specific coin
     */
    public void refreshCache(String coinGeckoId) {
        cacheLock.lock();
        try {
            System.out.println("[MarketDataCache] Refreshing cache for " + coinGeckoId);
            MarketData freshData = fetchMarketDataDirect(coinGeckoId);
            cache.put(coinGeckoId, new CachedMarketData(freshData));
            System.out.println("[MarketDataCache] Successfully refreshed cache for " + coinGeckoId);
        } catch (Exception e) {
            System.err.println("[MarketDataCache] Failed to refresh cache for " + coinGeckoId + ": " + e.getMessage());
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Direct API call method - fetches fresh data from CoinGecko
     */
    private MarketData fetchMarketDataDirect(String coinGeckoId) {
        try {
            // Get current price from CoinGecko
            String priceUrl = String.format("https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=usd&include_24hr_change=true&include_24hr_vol=true",
                coinGeckoId);
            System.out.println("[MarketDataCache] Fetching price from: " + priceUrl);
            String priceResponse = restTemplate.getForObject(priceUrl, String.class);
            JsonNode priceNode = objectMapper.readTree(priceResponse);
            JsonNode coinData = priceNode.get(coinGeckoId);
            if (coinData == null) {
                System.err.println("[MarketDataCache] No data found for " + coinGeckoId + " in price response: " + priceResponse);
                throw new RuntimeException("No data found for " + coinGeckoId);
            }

            MarketData marketData = new MarketData(coinGeckoId, coinData.get("usd").asDouble());

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
            System.out.println("[MarketDataCache] Fetching history from: " + historyUrl);
            String historyResponse = restTemplate.getForObject(historyUrl, String.class);
            JsonNode historyNode = objectMapper.readTree(historyResponse);
            JsonNode pricesArray = historyNode.get("prices");
            if (pricesArray == null) {
                System.err.println("[MarketDataCache] No prices array found for " + coinGeckoId + " in history response: " + historyResponse);
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
            System.err.println("[MarketDataCache] Error fetching market data for " + coinGeckoId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch market data for " + coinGeckoId, e);
        }
    }

    /**
     * Calculate technical indicators for market data
     */
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
            System.err.println("[MarketDataCache] Error calculating technical indicators for " + marketData.getSymbol() + ": " + e.getMessage());
        }
    }

    /**
     * Scheduled task to refresh cache for all holdings at 1:00 AM daily
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    public void refreshAllCaches() {
        System.out.println("[MarketDataCache] Starting scheduled cache refresh at 1:00 AM for all holdings");

        try {
            List<Holding> holdings = loadHoldings();
            int totalHoldings = holdings.size();
            int successCount = 0;
            int failCount = 0;

            System.out.println("[MarketDataCache] Refreshing cache for " + totalHoldings + " holdings");

            for (Holding holding : holdings) {
                try {
                    refreshCache(holding.getId());
                    successCount++;

                    // Add delay between API calls to respect rate limits
                    Thread.sleep(1200); // 1.2 seconds between calls

                } catch (Exception e) {
                    failCount++;
                    System.err.println("[MarketDataCache] Failed to refresh " + holding.getSymbol() +
                        " (" + holding.getId() + "): " + e.getMessage());
                }
            }

            System.out.println("[MarketDataCache] Cache refresh completed. Success: " + successCount +
                ", Failed: " + failCount + ", Total: " + totalHoldings);

        } catch (Exception e) {
            System.err.println("[MarketDataCache] Error during scheduled cache refresh: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Manual trigger for cache refresh (useful for testing)
     */
    public void refreshAllCachesManually() {
        System.out.println("[MarketDataCache] Manual cache refresh triggered");
        refreshAllCaches();
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        cacheLock.lock();
        try {
            int totalEntries = cache.size();
            int expiredEntries = 0;
            int freshEntries = 0;

            for (CachedMarketData cachedData : cache.values()) {
                if (cachedData.isExpired(18)) {
                    expiredEntries++;
                } else {
                    freshEntries++;
                }
            }

            return String.format("[MarketDataCache] Stats - Total: %d, Fresh: %d, Expired: %d",
                totalEntries, freshEntries, expiredEntries);
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Clear all cache entries
     */
    public void clearCache() {
        cacheLock.lock();
        try {
            int clearedCount = cache.size();
            cache.clear();
            System.out.println("[MarketDataCache] Cleared " + clearedCount + " cache entries");
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Remove expired cache entries
     */
    public void cleanupExpiredEntries() {
        cacheLock.lock();
        try {
            int initialSize = cache.size();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(12));
            int removedCount = initialSize - cache.size();

            if (removedCount > 0) {
                System.out.println("[MarketDataCache] Cleaned up " + removedCount + " expired cache entries");
            }
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Scheduled cleanup of expired entries every 6 hours
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 hours in milliseconds
    public void scheduledCleanup() {
        System.out.println("[MarketDataCache] Running scheduled cleanup of expired entries");
        cleanupExpiredEntries();
    }

    /**
     * Load holdings from configuration file
     */
    private List<Holding> loadHoldings() {
        try {
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            return holdings.getCryptos();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load holdings from holdings.json", e);
        }
    }
}
