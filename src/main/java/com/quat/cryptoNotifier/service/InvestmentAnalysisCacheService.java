package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for caching investment analysis summaries for consolidated email reporting.
 * Stores key information from each crypto's investment analysis to enable
 * sending a daily summary email with important insights across all holdings.
 */
@Service
public class InvestmentAnalysisCacheService {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CachedAnalysisSummary> analysisCache = new ConcurrentHashMap<>();
    private final ReentrantLock cacheLock = new ReentrantLock();
    private static final String CACHE_FILE_PATH = "cache/investment-analysis-cache.json";

    public InvestmentAnalysisCacheService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // Cache entry containing analysis summary with timestamp
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CachedAnalysisSummary {
        private AnalysisSummary summary;
        private LocalDateTime cacheTime;

        // Default constructor for JSON deserialization
        public CachedAnalysisSummary() {}

        public CachedAnalysisSummary(AnalysisSummary summary) {
            this.summary = summary;
            this.cacheTime = LocalDateTime.now();
        }

        public AnalysisSummary getSummary() {
            return summary;
        }

        public void setSummary(AnalysisSummary summary) {
            this.summary = summary;
        }

        public LocalDateTime getCacheTime() {
            return cacheTime;
        }

        public void setCacheTime(LocalDateTime cacheTime) {
            this.cacheTime = cacheTime;
        }

        public boolean isFromToday() {
            return cacheTime.toLocalDate().equals(LocalDateTime.now().toLocalDate());
        }
    }

    // Summary data structure containing key analysis information
    public static class AnalysisSummary {
        private String symbol;
        private String name;
        private String recommendation; // BUY/WAIT/DCA
        private String confidence; // HIGH/MEDIUM/LOW
        private String bottomLine;
        private String currentPrice;
        private String entryQuality;
        private String targetAllocation;
        private List<String> supportLevels;
        private List<String> resistanceLevels;
        private String shortTermOutlook;
        private String mediumTermOutlook;
        private String entryStrategy;
        private String dcaSchedule;
        private String currentSentiment;
        private List<String> positiveCatalysts;
        private List<String> riskFactors;
        private List<String> buyTriggers;
        private List<String> sellTriggers;
        private String keyBreakoutLevel;
        private String stopLossSuggestion;

        // Default constructor
        public AnalysisSummary() {}

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

        public String getConfidence() { return confidence; }
        public void setConfidence(String confidence) { this.confidence = confidence; }

        public String getBottomLine() { return bottomLine; }
        public void setBottomLine(String bottomLine) { this.bottomLine = bottomLine; }

        public String getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(String currentPrice) { this.currentPrice = currentPrice; }

        public String getEntryQuality() { return entryQuality; }
        public void setEntryQuality(String entryQuality) { this.entryQuality = entryQuality; }

        public String getTargetAllocation() { return targetAllocation; }
        public void setTargetAllocation(String targetAllocation) { this.targetAllocation = targetAllocation; }

        public List<String> getSupportLevels() { return supportLevels; }
        public void setSupportLevels(List<String> supportLevels) { this.supportLevels = supportLevels; }

        public List<String> getResistanceLevels() { return resistanceLevels; }
        public void setResistanceLevels(List<String> resistanceLevels) { this.resistanceLevels = resistanceLevels; }

        public String getShortTermOutlook() { return shortTermOutlook; }
        public void setShortTermOutlook(String shortTermOutlook) { this.shortTermOutlook = shortTermOutlook; }

        public String getMediumTermOutlook() { return mediumTermOutlook; }
        public void setMediumTermOutlook(String mediumTermOutlook) { this.mediumTermOutlook = mediumTermOutlook; }

        public String getEntryStrategy() { return entryStrategy; }
        public void setEntryStrategy(String entryStrategy) { this.entryStrategy = entryStrategy; }

        public String getDcaSchedule() { return dcaSchedule; }
        public void setDcaSchedule(String dcaSchedule) { this.dcaSchedule = dcaSchedule; }

        public String getCurrentSentiment() { return currentSentiment; }
        public void setCurrentSentiment(String currentSentiment) { this.currentSentiment = currentSentiment; }

        public List<String> getPositiveCatalysts() { return positiveCatalysts; }
        public void setPositiveCatalysts(List<String> positiveCatalysts) { this.positiveCatalysts = positiveCatalysts; }

        public List<String> getRiskFactors() { return riskFactors; }
        public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

        public List<String> getBuyTriggers() { return buyTriggers; }
        public void setBuyTriggers(List<String> buyTriggers) { this.buyTriggers = buyTriggers; }

        public List<String> getSellTriggers() { return sellTriggers; }
        public void setSellTriggers(List<String> sellTriggers) { this.sellTriggers = sellTriggers; }

        public String getKeyBreakoutLevel() { return keyBreakoutLevel; }
        public void setKeyBreakoutLevel(String keyBreakoutLevel) { this.keyBreakoutLevel = keyBreakoutLevel; }

        public String getStopLossSuggestion() { return stopLossSuggestion; }
        public void setStopLossSuggestion(String stopLossSuggestion) { this.stopLossSuggestion = stopLossSuggestion; }
    }

    /**
     * Initialize cache by loading from file on startup
     */
    @PostConstruct
    public void initializeCache() {
        loadCacheFromFile();
    }

    /**
     * Save cache to file before application shutdown
     */
    @PreDestroy
    public void shutdownCache() {
        saveCacheToFile();
    }

    /**
     * Cache analysis summary for a cryptocurrency
     */
    public void cacheAnalysisSummary(String symbol, Map<String, Object> analysisData) {
        try {
            AnalysisSummary summary = extractSummaryFromAnalysis(symbol, analysisData);
            CachedAnalysisSummary cachedSummary = new CachedAnalysisSummary(summary);
            
            cacheLock.lock();
            try {
                analysisCache.put(symbol, cachedSummary);
                System.out.println("[InvestmentAnalysisCache] Cached summary for " + symbol);
            } finally {
                cacheLock.unlock();
            }
            
            // Save to file immediately to persist the data
            saveCacheToFile();
            
        } catch (Exception e) {
            System.err.println("[InvestmentAnalysisCache] Error caching summary for " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all cached summaries from today
     */
    public List<AnalysisSummary> getTodaysAnalysisSummaries() {
        List<AnalysisSummary> summaries = new ArrayList<>();
        
        cacheLock.lock();
        try {
            for (CachedAnalysisSummary cachedSummary : analysisCache.values()) {
                if (cachedSummary.isFromToday()) {
                    summaries.add(cachedSummary.getSummary());
                }
            }
        } finally {
            cacheLock.unlock();
        }
        
        System.out.println("[InvestmentAnalysisCache] Retrieved " + summaries.size() + " summaries from today");
        return summaries;
    }

    /**
     * Clear cache entries older than today
     */
    public void clearOldEntries() {
        cacheLock.lock();
        try {
            analysisCache.entrySet().removeIf(entry -> !entry.getValue().isFromToday());
            System.out.println("[InvestmentAnalysisCache] Cleared old cache entries");
        } finally {
            cacheLock.unlock();
        }
        saveCacheToFile();
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        cacheLock.lock();
        try {
            int totalEntries = analysisCache.size();
            int todayEntries = (int) analysisCache.values().stream()
                .filter(CachedAnalysisSummary::isFromToday)
                .count();
            
            return String.format("Total entries: %d, Today's entries: %d", totalEntries, todayEntries);
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Extract key information from full analysis data
     */
    @SuppressWarnings("unchecked")
    private AnalysisSummary extractSummaryFromAnalysis(String symbol, Map<String, Object> analysisData) {
        AnalysisSummary summary = new AnalysisSummary();
        
        // Basic information
        summary.setSymbol(symbol);
        summary.setName((String) analysisData.getOrDefault("name", symbol));
        summary.setRecommendation((String) analysisData.getOrDefault("recommendation", "WAIT"));
        summary.setConfidence((String) analysisData.getOrDefault("confidence", "LOW"));
        summary.setBottomLine((String) analysisData.getOrDefault("bottom_line", "Analysis pending"));
        summary.setTargetAllocation((String) analysisData.getOrDefault("target_allocation", "TBD"));

        // Current price analysis
        Map<String, Object> priceAnalysis = (Map<String, Object>) analysisData.get("current_price_analysis");
        if (priceAnalysis != null) {
            summary.setEntryQuality((String) priceAnalysis.getOrDefault("entry_quality", "FAIR"));
            summary.setCurrentPrice((String) priceAnalysis.getOrDefault("current_price", "N/A"));
        }

        // Technical levels
        Map<String, Object> technicalLevels = (Map<String, Object>) analysisData.get("technical_levels");
        if (technicalLevels != null) {
            summary.setSupportLevels((List<String>) technicalLevels.getOrDefault("support_levels", new ArrayList<>()));
            summary.setResistanceLevels((List<String>) technicalLevels.getOrDefault("resistance_levels", new ArrayList<>()));
            summary.setKeyBreakoutLevel((String) technicalLevels.getOrDefault("key_breakout_level", "TBD"));
            summary.setStopLossSuggestion((String) technicalLevels.getOrDefault("stop_loss_suggestion", "TBD"));
        }

        // Outlook
        Map<String, Object> outlook = (Map<String, Object>) analysisData.get("outlook");
        if (outlook != null) {
            summary.setShortTermOutlook((String) outlook.getOrDefault("short_term", "Pending analysis"));
            summary.setMediumTermOutlook((String) outlook.getOrDefault("medium_term", "Pending analysis"));
        }

        // Strategy
        Map<String, Object> strategy = (Map<String, Object>) analysisData.get("strategy");
        if (strategy != null) {
            summary.setEntryStrategy((String) strategy.getOrDefault("entry_strategy", "Gradual entry"));
            summary.setDcaSchedule((String) strategy.getOrDefault("dca_schedule", "Weekly"));
        }

        // Market sentiment
        Map<String, Object> sentiment = (Map<String, Object>) analysisData.get("market_sentiment");
        if (sentiment != null) {
            summary.setCurrentSentiment((String) sentiment.getOrDefault("current_sentiment", "NEUTRAL"));
        }

        // Catalysts and risks
        Map<String, Object> catalystsRisks = (Map<String, Object>) analysisData.get("catalysts_and_risks");
        if (catalystsRisks != null) {
            summary.setPositiveCatalysts((List<String>) catalystsRisks.getOrDefault("positive_catalysts", new ArrayList<>()));
            summary.setRiskFactors((List<String>) catalystsRisks.getOrDefault("risk_factors", new ArrayList<>()));
        }

        // Key triggers
        Map<String, Object> keyTriggers = (Map<String, Object>) analysisData.get("key_triggers");
        if (keyTriggers != null) {
            summary.setBuyTriggers((List<String>) keyTriggers.getOrDefault("buy_triggers", new ArrayList<>()));
            summary.setSellTriggers((List<String>) keyTriggers.getOrDefault("sell_triggers", new ArrayList<>()));
        }

        return summary;
    }

    /**
     * Load cache from file
     */
    private void loadCacheFromFile() {
        cacheLock.lock();
        try {
            File cacheFile = new File(CACHE_FILE_PATH);
            if (!cacheFile.exists()) {
                System.out.println("[InvestmentAnalysisCache] No cache file found - starting with empty cache");
                return;
            }

            System.out.println("[InvestmentAnalysisCache] Loading cache from file: " + CACHE_FILE_PATH);

            Map<String, CachedAnalysisSummary> savedCache = objectMapper.readValue(
                cacheFile,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, CachedAnalysisSummary.class)
            );

            analysisCache.clear();
            int loadedCount = 0;
            int expiredCount = 0;

            for (Map.Entry<String, CachedAnalysisSummary> entry : savedCache.entrySet()) {
                CachedAnalysisSummary cachedSummary = entry.getValue();
                
                // Only load today's entries
                if (cachedSummary.isFromToday()) {
                    analysisCache.put(entry.getKey(), cachedSummary);
                    loadedCount++;
                } else {
                    expiredCount++;
                }
            }

            System.out.println("[InvestmentAnalysisCache] Cache loaded successfully. " +
                "Loaded: " + loadedCount + ", Expired (skipped): " + expiredCount);

        } catch (IOException e) {
            System.err.println("[InvestmentAnalysisCache] Error loading cache from file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[InvestmentAnalysisCache] Unexpected error loading cache: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Save cache to file
     */
    private void saveCacheToFile() {
        cacheLock.lock();
        try {
            // Create cache directory if it doesn't exist
            File cacheDir = new File("cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            File cacheFile = new File(CACHE_FILE_PATH);

            // Convert concurrent map to regular map for serialization
            Map<String, CachedAnalysisSummary> cacheToSave = new HashMap<>(analysisCache);

            objectMapper.writeValue(cacheFile, cacheToSave);

            System.out.println("[InvestmentAnalysisCache] Cache saved to file: " + CACHE_FILE_PATH +
                " (" + analysisCache.size() + " entries)");

        } catch (IOException e) {
            System.err.println("[InvestmentAnalysisCache] Error saving cache to file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[InvestmentAnalysisCache] Unexpected error saving cache: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cacheLock.unlock();
        }
    }
}
