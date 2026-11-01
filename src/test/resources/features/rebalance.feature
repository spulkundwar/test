Feature: Portfolio Rebalance

  # ---------------------------------------------------------------------------
  # Single-security share calculation (RebalancingService.calculate)
  # ---------------------------------------------------------------------------

  Scenario Outline: Verify share calculation
    Given total asset is <asset>
    And target percentage is <target>
    And current percentage is <current>
    And unit price is <price>
    When the shares are calculated
    Then action should be "<action>"
    And shares should be <shares>

    Examples:
      | asset  | target | current | price | action | shares |
      | 100000 | 20     | 10      | 150   | BUY    | 67     |
      | 100000 | 20     | 30      | 220   | SELL   | 45     |
      | 100000 | 20     | 20      | 90    | NONE   | 0      |

  Scenario Outline: Rounding and boundary values
    Given total asset is <asset>
    And target percentage is <target>
    And current percentage is <current>
    And unit price is <price>
    When the shares are calculated
    Then action should be "<action>"
    And shares should be <shares>

    Examples:
      | asset  | target | current | price | action | shares | note                           |
      | 100000 | 20     | 10      | 300   | BUY    | 33     | 33.33 rounds down              |
      | 100000 | 20     | 10      | 4000  | BUY    | 3      | 2.5 rounds half up             |
      | 100000 | 10     | 20      | 4000  | SELL   | 3      | same rounding on the sell side |
      | 100000 | 0      | 10      | 150   | SELL   | 67     | target 0 sells everything      |
      | 100000 | 100    | 90      | 200   | BUY    | 50     | target of exactly 100 is valid |
      | 100000 | 12.5   | 10      | 100   | BUY    | 25     | fractional percentage          |

  Scenario: Zero Asset Value
    Given total asset is 0
    And target percentage is 20
    And current percentage is 10
    And unit price is 150
    When the shares are calculated
    Then validation error should be "Asset value must be greater than 0"

  Scenario: Zero Price
    Given total asset is 100000
    And target percentage is 20
    And current percentage is 10
    And unit price is 0
    When the shares are calculated
    Then validation error should be "Price must be greater than 0"

  Scenario: Negative Asset Value
    Given total asset is -100000
    And target percentage is 20
    And current percentage is 10
    And unit price is 150
    When the shares are calculated
    Then validation error should be "Asset value must be greater than 0"

  Scenario: Negative Price
    Given total asset is 100000
    And target percentage is 20
    And current percentage is 10
    And unit price is -150
    When the shares are calculated
    Then validation error should be "Price must be greater than 0"

  Scenario: Invalid Allocation
    Given total asset is 100000
    And target percentage is 120
    And current percentage is 10
    And unit price is 150
    When the shares are calculated
    Then validation error should be "Target percentage cannot exceed 100"

  Scenario: Asset is validated before price
    Given total asset is 0
    And target percentage is 20
    And current percentage is 10
    And unit price is 0
    When the shares are calculated
    Then validation error should be "Asset value must be greater than 0"

  # ---------------------------------------------------------------------------
  # Account rebalance (RebalancingService.rebalance)
  # Validation rules themselves are covered above; these scenarios cover what
  # rebalance adds: iteration, security names, empty holdings, null checks and
  # all-or-nothing behaviour.
  # ---------------------------------------------------------------------------

  Scenario: Rebalance Account ABC to target weights
    Given account "ABC" has total assets 100000
    And the account holds:
      | security | target | current | price |
      | IBM      | 20     | 10      | 150   |
      | MSFT     | 20     | 20      | 90    |
      | ORCL     | 20     | 30      | 220   |
      | AAPL     | 20     | 20      | 450   |
      | HD       | 20     | 20      | 70    |
    When the account is rebalanced
    Then the shares to buy or sell should be:
      | security | action | shares |
      | IBM      | BUY    | 67     |
      | MSFT     | NONE   | 0      |
      | ORCL     | SELL   | 45     |
      | AAPL     | NONE   | 0      |
      | HD       | NONE   | 0      |


      # Assumption: An account that holds no securities has nothing to rebalance.
  # The system returns an empty list of trades, not an error. The rule that
  # target allocations must total 100% applies only once the account holds at
  # least one security.
  Scenario: Account with no holdings
    Given account "EMPTY" has total assets 100000
    When the account is rebalanced
    Then the shares to buy or sell should be:
      | security | action | shares |

  Scenario: Missing account
    Given no account is provided
    When the account is rebalanced
    Then validation error should be "Account is required"

  Scenario: Holding without a security name
    Given account "ABC" has total assets 100000
    And the account holds:
      | security | target | current | price |
      |          | 20     | 10      | 150   |
    When the account is rebalanced
    Then validation error should be "Security name is required"

  Scenario: One invalid holding fails the whole rebalance
    Given account "ABC" has total assets 100000
    And the account holds:
      | security | target | current | price |
      | IBM      | 20     | 10      | 150   |
      | MSFT     | 20     | 20      | 0     |
    When the account is rebalanced
    Then validation error should be "Price must be greater than 0"
    And no rebalance results should be returned

  Scenario: Account with zero total assets
    Given account "ABC" has total assets 0
    And the account holds:
      | security | target | current | price |
      | IBM      | 20     | 10      | 150   |
    When the account is rebalanced
    Then validation error should be "Asset value must be greater than 0"

  Scenario: Allocation less than 100 percent
    Given account "ABC" has total assets 100000
    And the account holds:
      | security | target | current | price |
      | IBM      | 20     | 10      | 150   |
      | MSFT     | 20     | 20      | 90    |
      | ORCL     | 20     | 30      | 220   |
      | AAPL     | 20     | 20      | 450   |
      | HD       | 10     | 20      | 70    |
    When the account is rebalanced
    Then validation error should be "Target allocation must total 100"
    And no rebalance results should be returned

  Scenario: Allocation greater than 100 percent
    Given account "ABC" has total assets 100000
    And the account holds:
      | security | target | current | price |
      | IBM      | 30     | 10      | 150   |
      | MSFT     | 30     | 20      | 90    |
      | ORCL     | 30     | 30      | 220   |
      | AAPL     | 20     | 20      | 450   |
      | HD       | 20     | 20      | 70    |
    When the account is rebalanced
    Then validation error should be "Target allocation must total 100"
    And no rebalance results should be returned

  Scenario: Fractional targets that total 100 are accepted
    Given account "ABC" has total assets 100000
    And the account holds:
      | security | target | current | price |
      | IBM      | 33.33  | 30      | 100   |
      | MSFT     | 33.33  | 30      | 100   |
      | ORCL     | 33.34  | 40      | 100   |
    When the account is rebalanced
    Then the shares to buy or sell should be:
      | security | action | shares |
      | IBM      | BUY    | 33     |
      | MSFT     | BUY    | 33     |
      | ORCL     | SELL   | 67     |


  Scenario: Negative Target Percentage
    Given total asset is 100000
    And target percentage is -10
    And current percentage is 10
    And unit price is 150
    When the shares are calculated
    Then validation error should be "Target percentage cannot be negative"