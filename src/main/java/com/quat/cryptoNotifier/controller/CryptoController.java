package com.quat.cryptoNotifier.controller;

import com.quat.cryptoNotifier.service.SchedulerService;
import com.quat.cryptoNotifier.service.AdvisoryEngineService;
import com.quat.cryptoNotifier.service.EmailService;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.Holdings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CryptoController {

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private AdvisoryEngineService advisoryEngineService;

    @Autowired
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/trigger-advisory")
    public String triggerAdvisory() {
        try {
            schedulerService.runManualAdvisory();
            return "Advisory triggered successfully";
        } catch (Exception e) {
            return "Error triggering advisory: " + e.getMessage();
        }
    }

    @GetMapping("/test-risk-analysis")
    public String testRiskAnalysis() {
        try {
            // This will only run the risk analysis phase
            schedulerService.runManualAdvisory();
            return "Risk & Opportunity Analysis completed successfully. Check your email and logs.";
        } catch (Exception e) {
            return "Error running risk analysis: " + e.getMessage();
        }
    }

    @GetMapping("/test-portfolio-health")
    public String testPortfolioHealthCheck() {
        try {
            // Load holdings
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            List<Holding> cryptos = holdings.getCryptos();

            // Generate portfolio health check
            Map<String, Object> healthData = advisoryEngineService.generatePortfolioHealthCheck(cryptos);

            // Send email
            emailService.sendPortfolioHealthCheck(cryptos, healthData);

            return "Portfolio Health Check completed successfully. Check your email for detailed analysis.";
        } catch (Exception e) {
            return "Error running portfolio health check: " + e.getMessage();
        }
    }

    @GetMapping("/test-portfolio-optimization")
    public String testPortfolioOptimization() {
        try {
            // Load holdings
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            List<Holding> cryptos = holdings.getCryptos();

            // Generate portfolio optimization analysis
            Map<String, Object> optimizationData = advisoryEngineService.generatePortfolioOptimizationAnalysis(cryptos);

            // Send email
            emailService.sendPortfolioOptimizationAnalysis(cryptos, optimizationData);

            return "Portfolio Optimization Analysis completed successfully. Check your email for detailed insights.";
        } catch (Exception e) {
            return "Error running portfolio optimization analysis: " + e.getMessage();
        }
    }

    @GetMapping("/test-investment-analysis")
    public String testInvestmentAnalysis(@RequestParam(defaultValue = "ETH") String symbol,
                                       @RequestParam(defaultValue = "Ethereum") String name) {
        try {
            // Create holding object from parameters
            Holding holding = new Holding();
            holding.setSymbol(symbol.toUpperCase());
            holding.setName(name);
            
            // Generate investment analysis
            Map<String, Object> investmentAnalysis = advisoryEngineService.generateInvestmentAnalysis(holding);

            // Send email with analysis
            emailService.sendInvestmentAnalysis(investmentAnalysis);

            return symbol.toUpperCase() + " Investment Analysis completed successfully. Check your email for detailed insights.";
        } catch (Exception e) {
            return "Error running " + symbol.toUpperCase() + " investment analysis: " + e.getMessage();
        }
    }

    @GetMapping("/test-eth-investment-analysis")
    public String testETHInvestmentAnalysis() {
        try {
            // Generate ETH investment analysis
            Map<String, Object> ethAnalysis = advisoryEngineService.generateETHInvestmentAnalysis();

            // Send email with analysis
            emailService.sendETHInvestmentAnalysis(ethAnalysis);

            return "ETH Investment Analysis completed successfully. Check your email for detailed insights.";
        } catch (Exception e) {
            return "Error running ETH investment analysis: " + e.getMessage();
        }
    }

    @GetMapping("/test-strategy-review")
    public String testStrategyAndTargetReview() {
        try {
            // Load holdings
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            List<Holding> cryptos = holdings.getCryptos();

            // Generate investment strategy analysis
            Map<String, Object> strategyAnalysis = advisoryEngineService.generateInvestmentStrategyAnalysis(cryptos);

            // Send email with analysis
            emailService.sendStrategyAndTargetReview(cryptos, strategyAnalysis);

            return "Investment Strategy & Target Review completed successfully. Check your email for detailed analysis of your strategy and price targets.";
        } catch (Exception e) {
            return "Error running strategy and target review: " + e.getMessage();
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Crypto Advisory Service is running";
    }

    @PostMapping("/reorder-holdings")
    public String reorderHoldingsByTotalAvgCost(@RequestParam(defaultValue = "desc") String order) {
        try {
            // Load current holdings
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            List<Holding> cryptos = holdings.getCryptos();

            if (cryptos == null || cryptos.isEmpty()) {
                return "No holdings found to reorder";
            }

            // Sort by total average cost (holdings * avgBuyPrice)
            Comparator<Holding> comparator = Comparator.comparingDouble(Holding::getTotalAvgCost);

            if ("desc".equalsIgnoreCase(order)) {
                comparator = comparator.reversed();
            }

            cryptos.sort(comparator);

            // Write back to holdings.json file with original formatting
            String holdingsPath = "src/main/resources/holdings.json";
            File holdingsFile = new File(holdingsPath);

            // Configure ObjectMapper to maintain original format
            ObjectMapper formatMapper = new ObjectMapper();
            formatMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, true);
            formatMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_NULL_MAP_VALUES, false);

            // Create a clean JSON structure that matches the original format exactly
            Map<String, Object> cleanOutput = new java.util.LinkedHashMap<>();
            Map<String, Object> portfolioMap = new java.util.LinkedHashMap<>();
            portfolioMap.put("lastBackup", holdings.getPortfolio().getLastBackup());

            // Create clean crypto objects with only the original fields in the correct order
            List<Map<String, Object>> cleanCryptos = cryptos.stream().map(crypto -> {
                Map<String, Object> cryptoMap = new java.util.LinkedHashMap<>();
                cryptoMap.put("id", crypto.getId());
                cryptoMap.put("symbol", crypto.getSymbol());
                cryptoMap.put("name", crypto.getName());
                cryptoMap.put("sector", crypto.getSector());
                cryptoMap.put("holdings", crypto.getHoldings());
                cryptoMap.put("avgBuyPrice", crypto.getAveragePrice());
                cryptoMap.put("expectedEntry", crypto.getExpectedEntry());
                cryptoMap.put("deepEntryPrice", crypto.getDeepEntryPrice());
                cryptoMap.put("targetPrice3Month", crypto.getTargetPrice3Month());
                cryptoMap.put("targetPriceLongTerm", crypto.getTargetPriceLongTerm());
                return cryptoMap;
            }).collect(java.util.stream.Collectors.toList());

            portfolioMap.put("cryptos", cleanCryptos);
            cleanOutput.put("portfolio", portfolioMap);

            formatMapper.writeValue(holdingsFile, cleanOutput);

            return String.format("Holdings successfully reordered by total average cost (%s). %d holdings processed.",
                               order.toUpperCase(), cryptos.size());

        } catch (Exception e) {
            return "Error reordering holdings: " + e.getMessage();
        }
    }

    @GetMapping("/holdings-summary")
    public Map<String, Object> getHoldingsSummary() {
        try {
            // Load holdings
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            List<Holding> cryptos = holdings.getCryptos();

            if (cryptos == null || cryptos.isEmpty()) {
                return Map.of("error", "No holdings found");
            }

            // Calculate summary statistics
            double totalAvgCost = cryptos.stream()
                .mapToDouble(Holding::getTotalAvgCost)
                .sum();

            return Map.of(
                "totalHoldings", cryptos.size(),
                "totalAverageCost", totalAvgCost,
                "holdings", cryptos.stream()
                    .map(crypto -> Map.of(
                        "symbol", crypto.getSymbol(),
                        "name", crypto.getName(),
                        "holdings", crypto.getHoldings(),
                        "avgBuyPrice", crypto.getAveragePrice(),
                        "totalAvgCost", crypto.getTotalAvgCost()
                    ))
                    .toList()
            );

        } catch (Exception e) {
            return Map.of("error", "Error retrieving holdings summary: " + e.getMessage());
        }
    }
}
