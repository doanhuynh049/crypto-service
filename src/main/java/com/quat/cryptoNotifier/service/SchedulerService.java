package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.Advisory;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.Holdings;
import com.quat.cryptoNotifier.model.MarketData;
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
    private DataProviderService dataProviderService;

    @Autowired
    private AdvisoryEngineService advisoryEngineService;

    @Autowired
    private EmailService emailService;

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
    }

    public void sendAdvisoriesForEachCrypto(List<Holding> holdings) throws InterruptedException {
        for (int i = 0; i < holdings.size(); i++) {
            Holding holding = holdings.get(i);
            System.out.println("Generating AI advisory for " + holding.getSymbol());
            Map<String, Object> riskAdvisories = advisoryEngineService.generateInvestmentAnalysis(holding);
            emailService.sendInvestmentAnalysis(riskAdvisories);
            Thread.sleep(30000);
            System.out.println("Completed AI analysis for " + holding.getSymbol());
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
