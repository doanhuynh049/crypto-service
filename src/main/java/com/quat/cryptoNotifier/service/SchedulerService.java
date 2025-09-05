package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.Advisory;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.Holdings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SchedulerService {

    @Autowired
    private AdvisoryEngineService advisoryEngineService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private InvestmentAnalysisCacheService investmentAnalysisCacheService;

    private final ObjectMapper objectMapper;

    public SchedulerService() {
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runDailyAdvisory() {
        System.out.println("Starting daily crypto advisory at " + LocalDateTime.now());
        
        try {
            // Load holdings
            List<Holding> holdings = loadHoldings();
            List<Advisory> advisoriesWithAI = new ArrayList<>();
            

            // First pass: Generate basic advisories for portfolio overview
            buildAndSendOverviewAdvisory(holdings);

            // Second pass: Generate AI investment analysis for each crypto
            sendAdvisoriesForEachCrypto(holdings);

            saveDailySnapshot(holdings, advisoriesWithAI);

            System.out.println("Daily advisory completed successfully");

        } catch (Exception e) {
            System.err.println("Error in daily advisory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void buildAndSendOverviewAdvisory(List<Holding> holdings) throws InterruptedException {
        // Generate and send portfolio table
        try {
            Map<String, Object> portfolioTableData = advisoryEngineService.generatePortfolioTable(holdings);
            emailService.sendPortfolioTable(holdings, portfolioTableData);
        } catch (Exception e) {
            System.err.println("Portfolio Table generation failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);

        try {
            Map<String, Object> riskAdvisories = advisoryEngineService.generateRiskOpportunityAnalysis(holdings);
            emailService.sendRiskOpportunityAnalysis(holdings, riskAdvisories);
        } catch (Exception e) {
            System.err.println("Risk Opportunity Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);

        try {
            Map<String, Object> healthAdvisories = advisoryEngineService.generatePortfolioHealthCheck(holdings);
            emailService.sendPortfolioHealthCheck(holdings, healthAdvisories);
        } catch (Exception e) {
            System.err.println("Portfolio Health Check failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);

        try {
            Map<String, Object> opportunityFinderAdvisories = advisoryEngineService.generateOpportunityFinder(holdings);
            emailService.sendOpportunityFinderAnalysis(holdings, opportunityFinderAdvisories);
        } catch (Exception e) {
            System.err.println("Opportunity Finder Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);

        try {
            Map<String, Object> portfolioOptimizationAdvisories = advisoryEngineService.generatePortfolioOptimizationAnalysis(holdings);
            emailService.sendPortfolioOptimizationAnalysis(holdings, portfolioOptimizationAdvisories);
        } catch (Exception e) {
            System.err.println("Portfolio Optimization Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);

        try {
            Map<String, Object> entryExitStrategies = advisoryEngineService.generateEntryExitStrategy(holdings);
            emailService.sendEntryExitStrategyAnalysis(holdings, entryExitStrategies);
        } catch (Exception e) {
            System.err.println("Entry & Exit Strategy Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);

        try {
            Map<String, Object> entryExitStrategies = advisoryEngineService.generateUSDTAllocationStrategy(holdings);
            emailService.sendUSDTAllocationStrategy(holdings, entryExitStrategies);
        } catch (Exception e) {
            System.err.println("USDT Allocation Strategy Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);

        try {
            Map<String, Object> strategyAnalysis = advisoryEngineService.generateInvestmentStrategyAnalysis(holdings);
            emailService.sendStrategyAndTargetReview(holdings, strategyAnalysis);
        } catch (Exception e) {
            System.err.println("Investment Strategy Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
        Thread.sleep(1000);
    }

    public void sendAdvisoriesForEachCrypto(List<Holding> holdings) throws InterruptedException {
        // Clear old cache entries at the start of a new analysis session
        investmentAnalysisCacheService.clearOldEntries();
        for (int i = 0; i < holdings.size(); i++) {
            try {
                Holding holding = holdings.get(i);
                if (holding.getSymbol() == null || holding.getSymbol().isEmpty() || holding.getSymbol().equals("USDT)")) {
                    System.out.println("Skipping holding with empty symbol at index " + i);
                    continue;
                }
                System.out.println("Generating AI advisory for " + holding.getSymbol());
                
                // Generate investment analysis
                Map<String, Object> analysisData = advisoryEngineService.generateInvestmentAnalysis(holding);
                
                // Cache the analysis summary for consolidated email
                investmentAnalysisCacheService.cacheAnalysisSummary(holding.getSymbol(), analysisData);
                
                // Send individual analysis email
                emailService.sendInvestmentAnalysis(analysisData);
                
                System.out.println("Completed AI analysis for " + holding.getSymbol());
            } catch (Exception e) {
                System.err.println("Error generating advisory for holding: " + holdings.get(i).getSymbol());
                e.printStackTrace();
                continue;
            } finally {
                // Pause between requests to avoid rate limits
                Thread.sleep(30000);
            }
        }
        
        // After all individual analyses are complete, send consolidated summary email
        sendConsolidatedAnalysisSummary();
    }

    /**
     * Send consolidated analysis summary email with all cached analysis summaries from today
     */
    private void sendConsolidatedAnalysisSummary() {
        try {
            List<InvestmentAnalysisCacheService.AnalysisSummary> summaries = 
                investmentAnalysisCacheService.getTodaysAnalysisSummaries();
            
            if (summaries.isEmpty()) {
                System.out.println("No analysis summaries available for consolidated email");
                return;
            }
            
            // Send consolidated email with all summaries
            emailService.sendConsolidatedInvestmentAnalysis(summaries);
            
            System.out.println("Sent consolidated analysis summary email with " + summaries.size() + " crypto analyses");
            
        } catch (Exception e) {
            System.err.println("Error sending consolidated analysis summary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runInvestmentAnalysisTest(List<Holding> holdings) {
        Holding ethHolding = holdings.stream()
            .filter(h -> "ETH".equalsIgnoreCase(h.getSymbol()))
            .findFirst()
            .orElse(null);
            
        // If ETH holding not found, create a default one for analysis
        if (ethHolding == null) {
            ethHolding = new Holding();
            ethHolding.setSymbol("ETH");
            ethHolding.setName("Ethereum");
        }
            
        Map<String, Object> riskAdvisories = advisoryEngineService.generateInvestmentAnalysis(ethHolding);
        emailService.sendInvestmentAnalysis(riskAdvisories);
    }

    // Manual trigger method for testing
    public void runManualAdvisory() {
        System.out.println("Running manual advisory...");
        runDailyAdvisory();
    }

    /**
     * Manual trigger for testing the investment analysis caching and consolidated email
     */
    public void runInvestmentAnalysisWithConsolidatedEmail() {
        System.out.println("Running manual investment analysis with consolidated email...");
        
        try {
            List<Holding> holdings = loadHoldings();
            
            // Test with a smaller subset for quicker testing (first 3 holdings)
            List<Holding> testHoldings = holdings.stream()
                .filter(h -> h.getSymbol() != null && !h.getSymbol().isEmpty() && !h.getSymbol().equals("USDT"))
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("Testing with " + testHoldings.size() + " holdings: " + 
                testHoldings.stream().map(Holding::getSymbol).collect(java.util.stream.Collectors.joining(", ")));
            
            sendAdvisoriesForEachCrypto(testHoldings);
            
        } catch (Exception e) {
            System.err.println("Error in manual investment analysis test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Holding> loadHoldings() {
        try {
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            return holdings.getCryptos();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load holdings from holdings.json", e);
        }
    }

    private void saveDailySnapshot(List<Holding> holdings, List<Advisory> advisories) {
        try {
            // Create logs directory if it doesn't exist
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filename = String.format("logs/advisory-%s.log", timestamp);

            try (FileWriter writer = new FileWriter(filename)) {
                writer.write("=== CRYPTO ADVISORY SNAPSHOT ===\n");
                writer.write("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n");

                for (int i = 0; i < holdings.size() && i < advisories.size(); i++) {
                    Holding holding = holdings.get(i);
                    Advisory advisory = advisories.get(i);

                    writer.write(String.format("--- %s ---\n", holding.getSymbol()));
                    writer.write(String.format("Price: $%.2f\n", advisory.getCurrentPrice()));
                    writer.write(String.format("P/L: $%.2f (%.2f%%)\n", advisory.getProfitLoss(), advisory.getProfitLossPercentage()));
                    writer.write(String.format("Action: %s\n", advisory.getAction()));
                    writer.write(String.format("Rationale: %s\n", advisory.getRationale()));
                    
                    if (advisory.getRsi() != null) {
                        writer.write(String.format("RSI: %.1f\n", advisory.getRsi()));
                    }
                    if (advisory.getMacd() != null) {
                        writer.write(String.format("MACD: %.4f\n", advisory.getMacd()));
                    }
                    
                    writer.write("\n");
                }
            }

            System.out.println("Daily snapshot saved to " + filename);

        } catch (IOException e) {
            System.err.println("Failed to save daily snapshot: " + e.getMessage());
        }
    }
}
