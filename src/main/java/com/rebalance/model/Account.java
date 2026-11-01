package com.rebalance.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account {

    private String name;
    private double totalAsset;
    private List<Security> holdings;

    public Account(String name,
                   double totalAsset,
                   List<Security> holdings) {
        this.name = name;
        this.totalAsset = totalAsset;
        this.holdings = holdings == null
                ? new ArrayList<>()
                : new ArrayList<>(holdings);
    }

    public String getName() {
        return name;
    }

    public double getTotalAsset() {
        return totalAsset;
    }

    public List<Security> getHoldings() {
        return Collections.unmodifiableList(holdings);
    }
}
