package com.quat.cryptoNotifier.service;

import com.quat.cryptoNotifier.config.AppConfig;
import com.quat.cryptoNotifier.model.Advisory;
import com.quat.cryptoNotifier.model.Holding;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendPortfolioOverview(List<Holding> holdings, Map<String, Object> advisories) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("advisories", advisories);
        
        String subject = String.format("📈 Portfolio Overview - %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "portfolio-overview", variables);
        System.out.println("Portfolio overview email sent successfully");
    }

    public void sendRiskOpportunityAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("holdings", holdings);
        variables.put("analysisData", analysisData);
        
        String subject = String.format("🎯 Risk & Opportunity Analysis - %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
        sendEmailWithTemplate(subject, "risk-opportunity-analysis", variables);
        System.out.println("Risk & Opportunity Analysis email sent successfully");
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
