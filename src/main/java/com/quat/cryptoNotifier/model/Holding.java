package com.quat.cryptoNotifier.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Holding {
    private String id;
    private String symbol;
    private String name;
    private double holdings;
    @JsonProperty("avgBuyPrice")
    private double averagePrice;
    private double expectedEntry;
    private double expectedDeepEntry;
    @JsonProperty("targetPrice3Month")
    private double targetPrice3Month;
    @JsonProperty("targetPriceLongTerm")
    private double targetPriceLongTerm;

    // Constructors
    public Holding() {}

    public Holding(String id, String symbol, String name, double holdings, double averagePrice, 
                   double expectedEntry, double expectedDeepEntry, double targetPrice3Month, double targetPriceLongTerm) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.holdings = holdings;
        this.averagePrice = averagePrice;
        this.expectedEntry = expectedEntry;
        this.expectedDeepEntry = expectedDeepEntry;
        this.targetPrice3Month = targetPrice3Month;
        this.targetPriceLongTerm = targetPriceLongTerm;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getHoldings() {
        return holdings;
    }

    public void setHoldings(double holdings) {
        this.holdings = holdings;
    }

    // Legacy method for backward compatibility
    @Deprecated
    public double getAmount() {
        return holdings;
    }

    @Deprecated
    public void setAmount(double amount) {
        this.holdings = amount;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public double getExpectedEntry() {
        return expectedEntry;
    }

    public void setExpectedEntry(double expectedEntry) {
        this.expectedEntry = expectedEntry;
    }

    public double getExpectedDeepEntry() {
        return expectedDeepEntry;
    }

    public void setDeepEntry(double expectedDeepEntry) {
        this.expectedDeepEntry = expectedDeepEntry;
    }

    public double getTargetPrice3Month() {
        return targetPrice3Month;
    }

    public void setTargetPrice3Month(double targetPrice3Month) {
        this.targetPrice3Month = targetPrice3Month;
    }

    public double getTargetPriceLongTerm() {
        return targetPriceLongTerm;
    }

    public void setTargetPriceLongTerm(double targetPriceLongTerm) {
        this.targetPriceLongTerm = targetPriceLongTerm;
    }

    // Legacy method for backward compatibility
    @Deprecated
    public double getTargetPrice() {
        return targetPrice3Month;
    }

    @Deprecated
    public void setTargetPrice(double targetPrice) {
        this.targetPrice3Month = targetPrice;
    }

    @Deprecated
    public double getMaxDrawdownPercentage() {
        return -15.0; // Default value for backward compatibility
    }

    @Deprecated
    public void setMaxDrawdownPercentage(double maxDrawdownPercentage) {
        // Legacy method - no action needed
    }

    // Calculated fields
    public double getCurrentValue(double currentPrice) {
        return holdings * currentPrice;
    }

    public double getInitialValue() {
        return holdings * averagePrice;
    }

    public double getProfitLoss(double currentPrice) {
        return getCurrentValue(currentPrice) - getInitialValue();
    }

    public double getProfitLossPercentage(double currentPrice) {
        return ((currentPrice - averagePrice) / averagePrice) * 100;
    }

    public double getPercentageToTarget(double currentPrice) {
        return ((targetPrice3Month - currentPrice) / currentPrice) * 100;
    }

    public double getPercentageToTargetLongTerm(double currentPrice) {
        return ((targetPriceLongTerm - currentPrice) / currentPrice) * 100;
    }

    @Override
    public String toString() {
        return "Holding{" +
                "id='" + id + '\'' +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", holdings=" + holdings +
                ", averagePrice=" + averagePrice +
                ", expectedEntry=" + expectedEntry +
                ", expectedDeepEntry=" + expectedDeepEntry +
                ", targetPrice3Month=" + targetPrice3Month +
                ", targetPriceLongTerm=" + targetPriceLongTerm +
                '}';
    }
}
