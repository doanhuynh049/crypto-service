package com.quat.cryptoNotifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String llmProvider;
    private String llmApiKey;
    private String smtpUser;
    private String smtpPass;
    private boolean smtpTls;
    private String mailFrom;
    private String mailTo;

    // Getters and Setters
    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public void setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getSmtpPass() {
        return smtpPass;
    }

    public void setSmtpPass(String smtpPass) {
        this.smtpPass = smtpPass;
    }

    public boolean isSmtpTls() {
        return smtpTls;
    }

    public void setSmtpTls(boolean smtpTls) {
        this.smtpTls = smtpTls;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }
}
