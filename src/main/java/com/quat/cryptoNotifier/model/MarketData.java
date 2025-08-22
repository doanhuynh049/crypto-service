package com.quat.cryptoNotifier.model;

import java.time.LocalDateTime;
import java.util.List;

public class MarketData {
    private String symbol;
    private double currentPrice;
    private double priceChange24h;
    private double priceChangePercentage24h;
    private double volume24h;
    private double marketCap;  // Added missing field
    private List<Double> prices;  // For technical indicators
    private LocalDateTime timestamp;

    // Technical indicators
    private Double rsi;
    private Double macd;
    private Double macdSignal;
    private Double sma20;
    private Double sma50;
    private Double sma200;

    public MarketData() {}

    public MarketData(String symbol, double currentPrice) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getPriceChange24h() {
        return priceChange24h;
    }

    public void setPriceChange24h(double priceChange24h) {
        this.priceChange24h = priceChange24h;
    }

    public double getPriceChangePercentage24h() {
        return priceChangePercentage24h;
    }

    public void setPriceChangePercentage24h(double priceChangePercentage24h) {
        this.priceChangePercentage24h = priceChangePercentage24h;
    }

    public double getVolume24h() {
        return volume24h;
    }

    public void setVolume24h(double volume24h) {
        this.volume24h = volume24h;
    }

    // Added missing marketCap getter and setter
    public double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(double marketCap) {
        this.marketCap = marketCap;
    }

    public List<Double> getPrices() {
        return prices;
    }

    public void setPrices(List<Double> prices) {
        this.prices = prices;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public Double getMacdSignal() {
        return macdSignal;
    }

    public void setMacdSignal(Double macdSignal) {
        this.macdSignal = macdSignal;
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
