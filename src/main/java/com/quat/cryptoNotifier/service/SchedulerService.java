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

    @Scheduled(cron = "0 30 7 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runDailyAdvisory() {
        System.out.println("Starting daily crypto advisory at " + LocalDateTime.now());
        
        try {
            // Load holdings
            List<Holding> holdings = loadHoldings();
            List<Advisory> advisories = new ArrayList<>();

            // Process each holding
            for (Holding holding : holdings) {
                try {
                    System.out.println("Processing " + holding.getSymbol());
                    
                    // Get market data
                    MarketData marketData = dataProviderService.getMarketData(holding.getSymbol());
                    
                    // Generate advisory
                    Advisory advisory = advisoryEngineService.generateAdvisory(holding, marketData);
                    advisories.add(advisory);
                    
                    System.out.println("Completed processing " + holding.getSymbol());
                    
                    // Add small delay between API calls
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    System.err.println("Error processing " + holding.getSymbol() + ": " + e.getMessage());
                }
            }

            // Send combined advisory email with all crypto advice
            emailService.sendCombinedAdvisory(holdings, advisories);

            // Save daily snapshot
            saveDailySnapshot(holdings, advisories);

            System.out.println("Daily advisory completed successfully");

        } catch (Exception e) {
            System.err.println("Error in daily advisory: " + e.getMessage());
            e.printStackTrace();
        }
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
            return holdings.getPositions();
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
