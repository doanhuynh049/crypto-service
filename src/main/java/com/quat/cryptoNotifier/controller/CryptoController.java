package com.quat.cryptoNotifier.controller;

import com.quat.cryptoNotifier.service.SchedulerService;
import com.quat.cryptoNotifier.service.AdvisoryEngineService;
import com.quat.cryptoNotifier.service.EmailService;
import com.quat.cryptoNotifier.model.Holding;
import com.quat.cryptoNotifier.model.Holdings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/health")
    public String health() {
        return "Crypto Advisory Service is running";
    }
}
