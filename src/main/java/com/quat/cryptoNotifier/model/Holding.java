package com.quat.cryptoNotifier.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Holding {
    private String symbol;
    private double amount;
    @JsonProperty("avgPrice")
    private double averagePrice;
    private double targetPrice;
    @JsonProperty("maxDrawdownPct")
    private double maxDrawdownPercentage;

    // Constructors
    public Holding() {}

    public Holding(String symbol, double amount, double averagePrice, double targetPrice, double maxDrawdownPercentage) {
        this.symbol = symbol;
        this.amount = amount;
        this.averagePrice = averagePrice;
        this.targetPrice = targetPrice;
        this.maxDrawdownPercentage = maxDrawdownPercentage;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public double getMaxDrawdownPercentage() {
        return maxDrawdownPercentage;
    }

    public void setMaxDrawdownPercentage(double maxDrawdownPercentage) {
        this.maxDrawdownPercentage = maxDrawdownPercentage;
    }

    // Calculated fields
    public double getCurrentValue(double currentPrice) {
        return amount * currentPrice;
    }

    public double getInitialValue() {
        return amount * averagePrice;
    }

    public double getProfitLoss(double currentPrice) {
        return getCurrentValue(currentPrice) - getInitialValue();
    }

    public double getProfitLossPercentage(double currentPrice) {
        return ((currentPrice - averagePrice) / averagePrice) * 100;
    }

    public double getPercentageToTarget(double currentPrice) {
        return ((targetPrice - currentPrice) / currentPrice) * 100;
    }

    @Override
    public String toString() {
        return "Holding{" +
                "symbol='" + symbol + '\'' +
                ", amount=" + amount +
                ", averagePrice=" + averagePrice +
                ", targetPrice=" + targetPrice +
                ", maxDrawdownPercentage=" + maxDrawdownPercentage +
                '}';
    }
}
