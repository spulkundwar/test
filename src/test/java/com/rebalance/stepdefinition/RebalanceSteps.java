package com.rebalance.stepdefinition;

import com.rebalance.model.Account;
import com.rebalance.model.RebalanceResult;
import com.rebalance.model.Security;
import com.rebalance.service.RebalancingService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RebalanceSteps {

    private final RebalancingService service =
            new RebalancingService();

    // single-security (calculate) state
    private double asset;
    private double target;
    private double current;
    private double price;
    private RebalanceResult result;

    // account (rebalance) state
    private String accountName;
    private boolean noAccount;
    private List<Security> holdings = new ArrayList<>();
    private List<RebalanceResult> accountResults;

    // shared
    private String errorMessage;

    // ---------------------------------------------------------------------
    // Given
    // ---------------------------------------------------------------------

    @Given("total asset is {double}")
    public void totalAsset(double asset) {
        this.asset = asset;
    }

    @Given("target percentage is {double}")
    public void targetPercentage(double target) {
        this.target = target;
    }

    @Given("current percentage is {double}")
    public void currentPercentage(double current) {
        this.current = current;
    }

    @Given("unit price is {double}")
    public void unitPrice(double price) {
        this.price = price;
    }

    @Given("account {string} has total assets {double}")
    public void accountHasTotalAssets(String name, double totalAssets) {
        this.accountName = name;
        this.asset = totalAssets;
        this.holdings = new ArrayList<>();
    }

    @Given("the account holds:")
    public void theAccountHolds(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            holdings.add(new Security(
                    row.get("security"),
                    Double.parseDouble(row.get("target")),
                    Double.parseDouble(row.get("current")),
                    Double.parseDouble(row.get("price"))));
        }
    }

    @Given("no account is provided")
    public void noAccountIsProvided() {
        this.noAccount = true;
    }

    // ---------------------------------------------------------------------
    // When
    // ---------------------------------------------------------------------

    @When("the shares are calculated")
    public void theSharesAreCalculated() {
        try {
            result = service.calculate(
                    asset,
                    target,
                    current,
                    price);
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
    }

    @When("the account is rebalanced")
    public void theAccountIsRebalanced() {
        try {
            Account account = noAccount
                    ? null
                    : new Account(accountName, asset, holdings);
            accountResults = service.rebalance(account);
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
    }

    // ---------------------------------------------------------------------
    // Then
    // ---------------------------------------------------------------------

    @Then("action should be {string}")
    public void verifyAction(String expected) {
        assertNoError();
        Assert.assertNotNull(result, "No calculation result");
        Assert.assertEquals(
                result.getAction(),
                expected);
    }

    @Then("shares should be {int}")
    public void verifyShares(int expected) {
        assertNoError();
        Assert.assertNotNull(result, "No calculation result");
        Assert.assertEquals(
                result.getShares(),
                expected);
    }

    @Then("the shares to buy or sell should be:")
    public void theSharesToBuyOrSellShouldBe(DataTable table) {
        assertNoError();
        Assert.assertNotNull(accountResults, "No rebalance results");

        List<Map<String, String>> expectedRows = table.asMaps();

        Assert.assertEquals(
                accountResults.size(),
                expectedRows.size(),
                "Unexpected number of rebalance results");

        for (Map<String, String> expected : expectedRows) {
            String ticker = expected.get("security");
            RebalanceResult actual = findResult(ticker);

            Assert.assertNotNull(
                    actual,
                    "No rebalance result for " + ticker);
            Assert.assertEquals(
                    actual.getAction(),
                    expected.get("action"),
                    ticker + " action");
            Assert.assertEquals(
                    actual.getShares(),
                    Integer.parseInt(expected.get("shares")),
                    ticker + " shares");
        }
    }

    @Then("validation error should be {string}")
    public void validationErrorShouldBe(String expectedError) {
        Assert.assertEquals(
                errorMessage,
                expectedError);
    }

    @Then("no rebalance results should be returned")
    public void noRebalanceResultsShouldBeReturned() {
        Assert.assertNull(
                accountResults,
                "Expected no results when rebalance fails");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void assertNoError() {
        Assert.assertNull(
                errorMessage,
                "Unexpected validation error: " + errorMessage);
    }

    private RebalanceResult findResult(String ticker) {
        for (RebalanceResult candidate : accountResults) {
            if (ticker.equals(candidate.getSecurity())) {
                return candidate;
            }
        }
        return null;
    }
}