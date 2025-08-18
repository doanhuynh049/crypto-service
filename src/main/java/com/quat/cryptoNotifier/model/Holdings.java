package com.quat.cryptoNotifier.model;

import java.util.List;

public class Holdings {
    private List<Holding> positions;

    public Holdings() {}

    public Holdings(List<Holding> positions) {
        this.positions = positions;
    }

    public List<Holding> getPositions() {
        return positions;
    }

    public void setPositions(List<Holding> positions) {
        this.positions = positions;
    }
}
