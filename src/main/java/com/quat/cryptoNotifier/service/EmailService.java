package com.quat.cryptoNotifier.service;

import com.quat.cryptoNotifier.config.AppConfig;
import com.quat.cryptoNotifier.model.Holding;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * Convert markdown-style bold formatting (**text**) to HTML bold tags
     */
    public String convertMarkdownToHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Convert **text** to <strong>text</strong>
        return text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
    }

    public void sendRiskOpportunityAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        // Process all text fields to convert markdown to HTML
        processAnalysisDataForHtml(analysisData);

        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("analysisData", analysisData);
        
        String subject = String.format("🎯 Risk & Opportunity Analysis - %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "risk-opportunity-analysis", variables);
        System.out.println("Risk & Opportunity Analysis email sent successfully");
    }

    /**
     * Process analysis data to convert markdown formatting to HTML
     */
    @SuppressWarnings("unchecked")
    private void processAnalysisDataForHtml(Map<String, Object> analysisData) {
        // Convert summary
        if (analysisData.containsKey("summary")) {
            analysisData.put("summary", convertMarkdownToHtml((String) analysisData.get("summary")));
        }

        // Process portfolio analysis
        if (analysisData.containsKey("portfolio_analysis")) {
            Map<String, Object> portfolioAnalysis = (Map<String, Object>) analysisData.get("portfolio_analysis");

            // Convert sector diversification text
            if (portfolioAnalysis.containsKey("sector_diversification")) {
                Map<String, Object> sectorDiv = (Map<String, Object>) portfolioAnalysis.get("sector_diversification");
                for (String key : sectorDiv.keySet()) {
                    if (sectorDiv.get(key) instanceof String) {
                        sectorDiv.put(key, convertMarkdownToHtml((String) sectorDiv.get(key)));
                    }
                }
            }

            // Convert other portfolio analysis fields
            String[] textFields = {"correlation_risks", "overexposure_concerns", "risk_reward_balance"};
            for (String field : textFields) {
                if (portfolioAnalysis.containsKey(field) && portfolioAnalysis.get(field) instanceof String) {
                    portfolioAnalysis.put(field, convertMarkdownToHtml((String) portfolioAnalysis.get(field)));
                }
            }

            // Process recommended adjustments
            if (portfolioAnalysis.containsKey("recommended_adjustments")) {
                List<Map<String, Object>> adjustments = (List<Map<String, Object>>) portfolioAnalysis.get("recommended_adjustments");
                for (Map<String, Object> adjustment : adjustments) {
                    if (adjustment.containsKey("action")) {
                        adjustment.put("action", convertMarkdownToHtml((String) adjustment.get("action")));
                    }
                    if (adjustment.containsKey("rationale")) {
                        adjustment.put("rationale", convertMarkdownToHtml((String) adjustment.get("rationale")));
                    }
                }
            }
        }

        // Process individual analysis
        if (analysisData.containsKey("individual_analysis")) {
            List<Map<String, Object>> individualAnalysis = (List<Map<String, Object>>) analysisData.get("individual_analysis");
            for (Map<String, Object> coin : individualAnalysis) {
                String[] coinFields = {"market_outlook_short", "market_outlook_long", "risk_factors", "upside_opportunities", "rationale"};
                for (String field : coinFields) {
                    if (coin.containsKey(field) && coin.get(field) instanceof String) {
                        coin.put(field, convertMarkdownToHtml((String) coin.get(field)));
                    }
                }
            }
        }
    }

    public void sendPortfolioHealthCheck(List<Holding> holdings, Map<String, Object> healthData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("healthData", healthData);
        
        String subject = String.format("🏥 Portfolio Health Check - %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "portfolio-health-check", variables);
        System.out.println("Portfolio Health Check email sent successfully");
    }

    public void sendOpportunityFinderAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("analysisData", analysisData);
        
        String subject = String.format("🔖 Opportunity Finder Analysis - %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "opportunity-finder", variables);
        System.out.println("Opportunity Finder Analysis email sent successfully");
    }

    public void sendPortfolioOptimizationAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("analysisData", analysisData);
        
        String subject = String.format("🔍 Portfolio Optimization Analysis - %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "portfolio-optimization", variables);
        System.out.println("Portfolio Optimization Analysis email sent successfully");
    }

    public void sendInvestmentAnalysis(Map<String, Object> analysisData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("analysisData", analysisData);
        
        String symbol = (String) analysisData.getOrDefault("symbol", "CRYPTO");
        String subject = String.format("🪙 %s Investment Analysis - %s", symbol,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "investment-analysis", variables);
        System.out.println(symbol + " Investment Analysis email sent successfully");
    }

    // Legacy method for ETH analysis
    public void sendETHInvestmentAnalysis(Map<String, Object> analysisData) {
        sendInvestmentAnalysis(analysisData);
    }

    public void sendEntryExitStrategyAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("analysisData", analysisData);
        
        String subject = String.format("🎯 Entry & Exit Strategy Analysis - %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "entry-exit-strategy", variables);
        System.out.println("Entry & Exit Strategy Analysis email sent successfully");
    }

    public void sendUSDTAllocationStrategy(List<Holding> holdings, Map<String, Object> analysisData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("analysisData", analysisData);
        
        String subject = String.format("💰 USDT Allocation Strategy - %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "usdt-allocation-strategy", variables);
        System.out.println("USDT Allocation Strategy email sent successfully");
    }

    public void sendPortfolioTable(List<Holding> holdings, Map<String, Object> portfolioData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("portfolioData", portfolioData);
        
        String subject = String.format("📊 Portfolio Table - %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "portfolio-table", variables);
        System.out.println("Portfolio Table email sent successfully");
    }

    public void sendStrategyAndTargetReview(List<Holding> holdings, Map<String, Object> strategyAnalysis) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("strategyAnalysis", strategyAnalysis);
        
        String subject = String.format("📈 Investment Strategy & Target Review - %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "strategy-target-review", variables);
        System.out.println("Investment Strategy & Target Review email sent successfully");
    }

    /**
     * Send consolidated investment analysis summary email with all crypto analyses
     */
    public void sendConsolidatedInvestmentAnalysis(List<InvestmentAnalysisCacheService.AnalysisSummary> analysisSummaries) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("analysisSummaries", analysisSummaries);
        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        
        String subject = String.format("📊 Daily Investment Analysis Summary - %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "consolidated-investment-analysis", variables);
        System.out.println("Consolidated Investment Analysis Summary email sent successfully with " + 
            analysisSummaries.size() + " crypto analyses");
    }

    /**
     * Common method to send emails with optional file attachments
     * This eliminates code duplication across all email sending methods
     */
    private void sendEmailWithTemplate(String subject, String templateName, 
                                     Map<String, Object> templateVariables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(subject);

            // Create context with all provided variables
            Context context = new Context();
            if (templateVariables != null) {
                for (Map.Entry<String, Object> entry : templateVariables.entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }
            
            // Always add timestamp
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Process template and set content
            String content = templateEngine.process(templateName, context);
            helper.setText(content, true);

            // Send the email
            mailSender.send(message);

            System.out.println("Email sent successfully: " + subject);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + subject, e);
        }
    }
}
