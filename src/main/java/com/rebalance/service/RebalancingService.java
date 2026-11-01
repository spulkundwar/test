package com.rebalance.service;

import com.rebalance.model.Account;
import com.rebalance.model.RebalanceResult;
import com.rebalance.model.Security;

import java.util.ArrayList;
import java.util.List;

public class RebalancingService {

    private static final double ALLOCATION_TOTAL = 100.0;
    private static final double ALLOCATION_TOLERANCE = 0.001;

    public List<RebalanceResult> rebalance(Account account) {

        if (account == null) {
            throw new IllegalArgumentException("Account is required");
        }

        List<RebalanceResult> results = new ArrayList<>();
        double totalTarget = 0;

        for (Security holding : account.getHoldings()) {

            if (holding == null
                    || holding.getSecurity() == null
                    || holding.getSecurity().isBlank()) {
                throw new IllegalArgumentException("Security name is required");
            }

            totalTarget += holding.getTargetPercentage();

            RebalanceResult calculated = calculate(
                    account.getTotalAsset(),
                    holding.getTargetPercentage(),
                    holding.getCurrentPercentage(),
                    holding.getUnitPrice());

            results.add(new RebalanceResult(
                    holding.getSecurity(),
                    calculated.getAction(),
                    calculated.getShares()));
        }

        if (!results.isEmpty()
                && Math.abs(totalTarget - ALLOCATION_TOTAL) > ALLOCATION_TOLERANCE) {
            throw new IllegalArgumentException(
                    "Target allocation must total 100");
        }

        return results;
    }

    public RebalanceResult calculate(
            double totalAsset,
            double targetPercent,
            double currentPercent,
            double price) {

        if (totalAsset <= 0) {
            throw new IllegalArgumentException(
                    "Asset value must be greater than 0");
        }

        if (price <= 0) {
            throw new IllegalArgumentException(
                    "Price must be greater than 0");
        }

        if (targetPercent < 0) {
            throw new IllegalArgumentException(
                    "Target percentage cannot be negative");
        }

        if (targetPercent > 100) {
            throw new IllegalArgumentException(
                    "Target percentage cannot exceed 100");
        }

        double targetValue =
                totalAsset * targetPercent / 100;

        double currentValue =
                totalAsset * currentPercent / 100;

        double difference =
                targetValue - currentValue;

        String action;

        if (difference > 0) {
            action = "BUY";
        } else if (difference < 0) {
            action = "SELL";
        } else {
            action = "NONE";
        }

        int shares =
                (int) Math.round(
                        Math.abs(difference / price));

        return new RebalanceResult(action, shares);
    }
}
