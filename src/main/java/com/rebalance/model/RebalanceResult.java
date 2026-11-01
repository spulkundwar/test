package com.rebalance.model;

public class RebalanceResult {

    private String security;
    private String action;
    private int shares;

    public RebalanceResult(String action, int shares) {
        this(null, action, shares);
    }

    public RebalanceResult(String security, String action, int shares) {
        this.security = security;
        this.action = action;
        this.shares = shares;
    }

    public String getSecurity() {
        return security;
    }

    public String getAction() {
        return action;
    }

    public int getShares() {
        return shares;
    }
}
