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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public void sendTokenAdvisory(Holding holding, Advisory advisory) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("📊 %s Daily Advisory - %s", 
                holding.getSymbol(), 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holding", holding);
            context.setVariable("advisory", advisory);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("token-advisory", context);
            helper.setText(content, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send token advisory email for " + holding.getSymbol(), e);
        }
    }

    public void sendCombinedAdvisory(List<Holding> holdings, List<Advisory> advisories) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("📊 Daily Crypto Advisory - %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("advisories", advisories);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Calculate portfolio totals
            double totalValue = 0;
            double totalProfitLoss = 0;
            double totalInitialValue = 0;

            for (Advisory advisory : advisories) {
                Holding holding = holdings.stream()
                    .filter(h -> h.getSymbol().equals(advisory.getSymbol()))
                    .findFirst()
                    .orElse(null);
                
                if (holding != null) {
                    totalValue += holding.getCurrentValue(advisory.getCurrentPrice());
                    totalProfitLoss += advisory.getProfitLoss();
                    totalInitialValue += holding.getInitialValue();
                }
            }

            double totalProfitLossPercentage = totalInitialValue > 0 ? (totalProfitLoss / totalInitialValue) * 100 : 0;

            context.setVariable("totalValue", totalValue);
            context.setVariable("totalProfitLoss", totalProfitLoss);
            context.setVariable("totalProfitLossPercentage", totalProfitLossPercentage);
            context.setVariable("totalInitialValue", totalInitialValue);

            String content = templateEngine.process("combined-advisory", context);
            helper.setText(content, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send combined advisory email", e);
        }
    }

    public void sendPortfolioOverview(List<Holding> holdings, Map<String, Object> advisories) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("📈 Portfolio Overview - %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("advisories", advisories);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("portfolio-overview", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("Portfolio overview email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send portfolio overview email", e);
        }
    }

    public void sendRiskOpportunityAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("🎯 Risk & Opportunity Analysis - %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("analysisData", analysisData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("risk-opportunity-analysis", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("Risk & Opportunity Analysis email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send risk & opportunity analysis email", e);
        }
    }

    public void sendPortfolioHealthCheck(List<Holding> holdings, Map<String, Object> healthData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("🏥 Portfolio Health Check - %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("healthData", healthData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("portfolio-health-check", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("Portfolio Health Check email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send portfolio health check email", e);
        }
    }

    public void sendOpportunityFinderAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("🔖 Opportunity Finder Analysis - %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("analysisData", analysisData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("opportunity-finder", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("Opportunity Finder Analysis email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send opportunity finder analysis email", e);
        }
    }

    public void sendPortfolioOptimizationAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("🔍 Portfolio Optimization Analysis - %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("analysisData", analysisData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("portfolio-optimization", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("Portfolio Optimization Analysis email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send portfolio optimization analysis email", e);
        }
    }

    public void sendInvestmentAnalysis(Map<String, Object> analysisData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            
            String symbol = (String) analysisData.getOrDefault("symbol", "CRYPTO");
            helper.setSubject(String.format("\uD83E\uDE99 %s Investment Analysis - %s", symbol,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("analysisData", analysisData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("investment-analysis", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println(symbol + " Investment Analysis email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send investment analysis email", e);
        }
    }

    // Legacy method for ETH analysis
    public void sendETHInvestmentAnalysis(Map<String, Object> analysisData) {
        sendInvestmentAnalysis(analysisData);
    }

    public void sendEntryExitStrategyAnalysis(List<Holding> holdings, Map<String, Object> analysisData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("🎯 Entry & Exit Strategy Analysis - %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            
            // Ensure analysisData has required keys to prevent template errors
            if (analysisData == null) {
                analysisData = new HashMap<>();
            }
            if (!analysisData.containsKey("strategies")) {
                analysisData.put("strategies", new ArrayList<>());
            }
            if (!analysisData.containsKey("portfolio")) {
                analysisData.put("portfolio", new HashMap<>());
            }
            
            context.setVariable("analysisData", analysisData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("entry-exit-strategy", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("Entry & Exit Strategy Analysis email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send entry & exit strategy analysis email", e);
        }
    }

    public void sendUSDTAllocationStrategy(List<Holding> holdings, Map<String, Object> analysisData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("💰 USDT Allocation Strategy - %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("analysisData", analysisData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("usdt-allocation-strategy", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("USDT Allocation Strategy email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send USDT allocation strategy email", e);
        }
    }

    public void sendPortfolioTable(List<Holding> holdings, Map<String, Object> portfolioData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appConfig.getMailFrom());
            helper.setTo(appConfig.getMailTo());
            helper.setSubject(String.format("📊 Portfolio Table - %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

            Context context = new Context();
            context.setVariable("holdings", holdings);
            context.setVariable("portfolioData", portfolioData);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String content = templateEngine.process("portfolio-table", context);
            helper.setText(content, true);

            mailSender.send(message);

            System.out.println("Portfolio Table email sent successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send portfolio table email", e);
        }
    }
}
