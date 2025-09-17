package com.quat.cryptoNotifier.controller;

import com.quat.cryptoNotifier.service.SchedulerService;
import com.quat.cryptoNotifier.service.AdvisoryEngineService;
import com.quat.cryptoNotifier.service.EmailService;
import com.quat.cryptoNotifier.service.DataProviderService;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.Holdings;
import com.quat.cryptoNotifier.model.MarketData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

// Apache POI imports for Excel generation
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @Autowired
    private DataProviderService dataProviderService;

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
    public String reorderHoldingsByTotalAvgCost(@RequestParam(defaultValue = "desc") String order,
                                               @RequestParam(defaultValue = "total_avg_cost") String sortBy) {
        try {
            // Load current holdings
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            List<Holding> cryptos = holdings.getCryptos();

            if (cryptos == null || cryptos.isEmpty()) {
                return "No holdings found to reorder";
            }

            // Choose sorting method based on sortBy parameter
            Comparator<Holding> comparator;
            String sortDescription;

            if ("total_current_value".equalsIgnoreCase(sortBy)) {
                // Sort by total current value (holdings * current price)
                comparator = Comparator.comparingDouble(this::calculateTotalCurrentValue);
                sortDescription = "total current value";
            } else {
                // Default: Sort by total average cost (holdings * avgBuyPrice)
                comparator = Comparator.comparingDouble(Holding::getTotalAvgCost);
                sortDescription = "total average cost";
            }

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

            return String.format("Holdings successfully reordered by %s (%s). %d holdings processed.",
                               sortDescription, order.toUpperCase(), cryptos.size());

        } catch (Exception e) {
            return "Error reordering holdings: " + e.getMessage();
        }
    }

    @GetMapping("/export-holdings-xlsx")
    public ResponseEntity<byte[]> exportHoldingsToXlsx(@RequestParam(defaultValue = "desc") String order,
                                                       @RequestParam(defaultValue = "total_current_value") String sortBy) {
        try {
            // Load current holdings
            ClassPathResource resource = new ClassPathResource("holdings.json");
            Holdings holdings = objectMapper.readValue(resource.getInputStream(), Holdings.class);
            List<Holding> cryptos = holdings.getCryptos();

            if (cryptos == null || cryptos.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Sort holdings based on specified criteria
            Comparator<Holding> comparator;
            if ("total_current_value".equalsIgnoreCase(sortBy)) {
                comparator = Comparator.comparingDouble(this::calculateTotalCurrentValue);
            } else {
                comparator = Comparator.comparingDouble(Holding::getTotalAvgCost);
            }

            if ("desc".equalsIgnoreCase(order)) {
                comparator = comparator.reversed();
            }
            cryptos.sort(comparator);

            // Generate XLSX file
            byte[] excelData = generateHoldingsExcel(cryptos);

            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = String.format("portfolio-holdings_%s.xlsx", timestamp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            System.err.println("Error exporting holdings to XLSX: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Calculate total current value for a holding
     */
    private double calculateTotalCurrentValue(Holding holding) {
        try {
            MarketData marketData = dataProviderService.getMarketData(holding.getId());
            return holding.getHoldings() * marketData.getCurrentPrice();
        } catch (Exception e) {
            System.err.println("Error fetching current price for " + holding.getSymbol() + ": " + e.getMessage());
            // Fallback to total average cost if current price unavailable
            return holding.getTotalAvgCost();
        }
    }

    /**
     * Generate Excel file with portfolio holdings
     */
    private byte[] generateHoldingsExcel(List<Holding> cryptos) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Portfolio Holdings");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create data style for numbers
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Symbol", "Name", "Exchange", "Sector", "Industry",
            "Shares", "Avg Buy Price", "Conservative Entry",
            "Target 3M", "Target Long Term", "Platform"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Populate data rows
        int rowNum = 1;
        for (Holding holding : cryptos) {
            Row row = sheet.createRow(rowNum++);

            // Symbol
            row.createCell(0).setCellValue(holding.getSymbol());

            // Name
            row.createCell(1).setCellValue(holding.getName());

            // Exchange (assuming CoinGecko for crypto)
            row.createCell(2).setCellValue("CoinGecko");

            // Sector
            row.createCell(3).setCellValue(holding.getSector() != null ? holding.getSector() : "Cryptocurrency");

            // Industry (using sector as industry for crypto)
            row.createCell(4).setCellValue(holding.getSector() != null ? holding.getSector() : "Blockchain");

            // Shares (holdings amount)
            Cell sharesCell = row.createCell(5);
            sharesCell.setCellValue(holding.getHoldings());
            sharesCell.setCellStyle(numberStyle);

            // Avg Buy Price
            Cell avgPriceCell = row.createCell(6);
            avgPriceCell.setCellValue(holding.getAveragePrice());
            avgPriceCell.setCellStyle(currencyStyle);

            // Conservative Entry (using expectedEntry)
            Cell entryCell = row.createCell(7);
            entryCell.setCellValue(holding.getExpectedEntry());
            entryCell.setCellStyle(currencyStyle);

            // Target 3M
            Cell target3MCell = row.createCell(8);
            target3MCell.setCellValue(holding.getTargetPrice3Month());
            target3MCell.setCellStyle(currencyStyle);

            // Target Long Term
            Cell targetLTCell = row.createCell(9);
            targetLTCell.setCellValue(holding.getTargetPriceLongTerm());
            targetLTCell.setCellStyle(currencyStyle);

            // Platform (generic for crypto)
            row.createCell(10).setCellValue("Crypto Exchange");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
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
