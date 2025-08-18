package com.quat.cryptoNotifier.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Advisory {
    private String symbol;
    private String action;
    private String rationale;
    private String levels;
    @JsonProperty("risk_notes")
    private String riskNotes;

    // Analysis data
    private double currentPrice;
    private double profitLoss;
    private double profitLossPercentage;
    private double percentageToTarget;
    private Double rsi;
    private Double macd;
    private Double sma20;
    private Double sma50;
    private Double sma200;

    public Advisory() {}

    public Advisory(String symbol) {
        this.symbol = symbol;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getLevels() {
        return levels;
    }

    public void setLevels(String levels) {
        this.levels = levels;
    }

    public String getRiskNotes() {
        return riskNotes;
    }

    public void setRiskNotes(String riskNotes) {
        this.riskNotes = riskNotes;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(double profitLoss) {
        this.profitLoss = profitLoss;
    }

    public double getProfitLossPercentage() {
        return profitLossPercentage;
    }

    public void setProfitLossPercentage(double profitLossPercentage) {
        this.profitLossPercentage = profitLossPercentage;
    }

    public double getPercentageToTarget() {
        return percentageToTarget;
    }

    public void setPercentageToTarget(double percentageToTarget) {
        this.percentageToTarget = percentageToTarget;
    }

    public Double getRsi() {
        return rsi;
    }

    public void setRsi(Double rsi) {
        this.rsi = rsi;
    }

    public Double getMacd() {
        return macd;
    }

    public void setMacd(Double macd) {
        this.macd = macd;
    }

    public Double getSma20() {
        return sma20;
    }

    public void setSma20(Double sma20) {
        this.sma20 = sma20;
    }

    public Double getSma50() {
        return sma50;
    }

    public void setSma50(Double sma50) {
        this.sma50 = sma50;
    }

    public Double getSma200() {
        return sma200;
    }

    public void setSma200(Double sma200) {
        this.sma200 = sma200;
    }
}
