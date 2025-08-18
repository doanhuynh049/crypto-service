package com.quat.cryptoNotifier.model;

import java.util.List;

public class Holdings {
    private Portfolio portfolio;

    public Holdings() {}

    public Holdings(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    // Convenience method to get cryptos directly
    public List<Holding> getCryptos() {
        return portfolio != null ? portfolio.getCryptos() : null;
    }

    // Legacy method for backward compatibility
    @Deprecated
    public List<Holding> getPositions() {
        return getCryptos();
    }
}
