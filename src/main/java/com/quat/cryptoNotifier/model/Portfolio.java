package com.quat.cryptoNotifier.model;

import java.util.List;

public class Portfolio {
    private String lastBackup;
    private List<Holding> cryptos;

    public Portfolio() {}

    public Portfolio(String lastBackup, List<Holding> cryptos) {
        this.lastBackup = lastBackup;
        this.cryptos = cryptos;
    }

    public String getLastBackup() {
        return lastBackup;
    }

    public void setLastBackup(String lastBackup) {
        this.lastBackup = lastBackup;
    }

    public List<Holding> getCryptos() {
        return cryptos;
    }

    public void setCryptos(List<Holding> cryptos) {
        this.cryptos = cryptos;
    }
}
