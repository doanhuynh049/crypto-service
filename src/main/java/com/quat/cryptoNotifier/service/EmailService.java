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
}
