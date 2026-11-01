package com.rebalance.model;
public class Security
{

    private String security;
    private double targetPercentage;
    private double currentPercentage;
    private double unitPrice;

    public Security(String security,
                    double targetPercentage,
                    double currentPercentage,
                    double unitPrice) {
        this.security = security;
        this.targetPercentage = targetPercentage;
        this.currentPercentage = currentPercentage;
        this.unitPrice = unitPrice;
    }

    public String getSecurity() {
        return security;
    }

    public double getTargetPercentage() {
        return targetPercentage;
    }

    public double getCurrentPercentage() {
        return currentPercentage;
    }

    public double getUnitPrice() {
        return unitPrice;
    }
}
