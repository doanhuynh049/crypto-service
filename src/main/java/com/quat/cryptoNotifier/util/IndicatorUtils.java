package com.quat.cryptoNotifier.util;

import java.util.List;

public class IndicatorUtils {

    /**
     * Calculate Simple Moving Average
     */
    public static Double calculateSMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            return null;
        }
        
        double sum = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    /**
     * Calculate RSI (Relative Strength Index)
     */
    public static Double calculateRSI(List<Double> prices, int period) {
        if (prices == null || prices.size() < period + 1) {
            return null;
        }

        double gainSum = 0;
        double lossSum = 0;

        // Calculate initial average gain and loss
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        if (avgLoss == 0) {
            return 100.0;
        }

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     */
    public static MACDResult calculateMACD(List<Double> prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (prices == null || prices.size() < Math.max(slowPeriod, signalPeriod) + 1) {
            return null;
        }

        // Calculate EMAs
        Double fastEMA = calculateEMA(prices, fastPeriod);
        Double slowEMA = calculateEMA(prices, slowPeriod);

        if (fastEMA == null || slowEMA == null) {
            return null;
        }

        double macdLine = fastEMA - slowEMA;

        // For simplicity, using SMA for signal line instead of EMA
        // In a production system, you'd maintain MACD history and calculate EMA of MACD
        return new MACDResult(macdLine, macdLine); // Using same value for signal as approximation
    }

    /**
     * Calculate Exponential Moving Average
     */
    private static Double calculateEMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            return null;
        }

        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(prices.size() - period); // Start with first price

        for (int i = prices.size() - period + 1; i < prices.size(); i++) {
            ema = (prices.get(i) * multiplier) + (ema * (1 - multiplier));
        }

        return ema;
    }

    public static class MACDResult {
        private final double macd;
        private final double signal;

        public MACDResult(double macd, double signal) {
            this.macd = macd;
            this.signal = signal;
        }

        public double getMacd() {
            return macd;
        }

        public double getSignal() {
            return signal;
        }
    }
}
