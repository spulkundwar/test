# Portfolio Rebalance Automation Assessment

Java implementation of the rebalancing exercise, with Cucumber BDD tests run through TestNG.

The assignment asks: given account **ABC** ($100,000, 100% vested) and five securities, produce the number of shares to **buy** or **sell** so each holding reaches its target weight. Tests must confirm that share counts and actions are correct. Assumptions are recorded in [ASSUMPTIONS.md](ASSUMPTIONS.md).

## Overview

The application under test is a small in-process service, not a UI or HTTP API. Automated tests call `RebalancingService` directly.

| Layer | Component | Role |
| --- | --- | --- |
| Domain | `Account` | Named account, total asset value, list of holdings |
| Domain | `Security` | Ticker, target %, current %, unit price |
| Domain | `RebalanceResult` | Ticker, action (`BUY` / `SELL` / `NONE`), whole-share quantity |
| Logic | `RebalancingService.calculate` | One-security share math and field validation |
| Logic | `RebalancingService.rebalance` | Walk all holdings, enforce a 100% target mix, return trades |
| BDD | `src/test/resources/features/rebalance.feature` | Scenarios for happy path, rounding, validation, and account-level rules |
| Glue | `RebalanceSteps.java` | Cucumber step definitions |
| Runner | `TestRunner` + `testng.xml` | TestNG suite that loads the feature files |

**Share formula** (see [ASSUMPTIONS.md](ASSUMPTIONS.md) for rounding and edge cases):

```
targetValue  = totalAsset × target% / 100
currentValue = totalAsset × current% / 100
difference   = targetValue − currentValue
action       = BUY if difference > 0, SELL if difference < 0, NONE if 0
shares       = round(|difference| / unitPrice)   // Java Math.round, int shares
```

Assignment sample for account ABC (`$100,000`):

| Security | Target % | Current % | Price | Action | Shares |
| --- | --- | --- | --- | --- | --- |
| IBM | 20 | 10 | 150 | BUY | 67 |
| MSFT | 20 | 20 | 90 | NONE | 0 |
| ORCL | 20 | 30 | 220 | SELL | 45 |
| AAPL | 20 | 20 | 450 | NONE | 0 |
| HD | 20 | 20 | 70 | NONE | 0 |

Manual cases mapped to the feature file live in `docs/Manual_Test_Cases_Feature_Mapping.xlsx`.

## Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | **17 or newer** | `pom.xml` compiles with `--release 17`. JDK 21 (for example Eclipse Temurin 21) is fine. |
| Apache Maven | 3.8+ | Uses compiler plugin 3.13.0 and Surefire 3.2.5. |
| OS | Windows / macOS / Linux | Windows PowerShell notes below. |

Confirm the session actually sees Java and Maven:

```powershell
java -version
mvn -version
```

Both should report a JDK **17+**. `mvn -version` prints the JVM Maven will use; that is the one that matters for `mvn test`.

### Windows: refresh `JAVA_HOME` and `PATH` in the current PowerShell

User-level `JAVA_HOME` is sometimes set, but an already-open terminal still has an empty or stale `JAVA_HOME` / `PATH`. Maven then fails with “JAVA_HOME not found” or picks the wrong `java.exe`.

Run this in **that** PowerShell window before Maven (not required if `java -version` and `mvn -version` already look correct):

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
}
$env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
```

Then:

```powershell
echo $env:JAVA_HOME
java -version
mvn -version
```

`JAVA_HOME` should point at a JDK 17+ install (for example `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot` or a JDK 17 path), and `mvn -version` should show that same Java.

Permanent setup (one-time, then open a **new** terminal):

1. Install a JDK 17+ and set a **User** environment variable `JAVA_HOME` to the JDK root (not the `bin` folder).
2. Add `%JAVA_HOME%\bin` to the **User** `Path`.
3. Restart the IDE and any terminals.

### IntelliJ Java settings

This project’s IntelliJ module is currently set to language level **JDK 21** (Temurin). That is compatible with `--release 17` in `pom.xml`.

If the IDE and the terminal disagree:

- **Project SDK / Gradle JVM is unused** — this is a Maven project. Set **Project SDK** to a JDK 17+.
- **Maven runner**: Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → **JRE** should be the same 17+ JDK, or “Use Project JDK”.
- Language level can stay 17 (matches `pom.xml`) or 21 (still able to compile release 17).

No extra `MAVEN_OPTS` or Surefire `argLine` JVM flags are required.

## Build and run

From the project folder (`PortfolioRebalanceAutomationAssessment`):

### 1. Install dependencies and compile

```powershell
mvn -q dependency:resolve
mvn -q compile test-compile
```

The first Maven invocation downloads Cucumber 7.18.1, TestNG 7.10.2, SLF4J, and the compiler/Surefire plugins.

### 2. Run the test suite

Preferred (uses `testng.xml` from `pom.xml`):

```powershell
mvn test
```

Equivalent explicit Surefire override (same suite):

```powershell
mvn test "-Dsurefire.suiteXmlFiles=testng.xml"
```

On Windows PowerShell, quote the `-D` flag as shown so PowerShell does not treat it as a stray token.

### 3. Reports

| Report | Path |
| --- | --- |
| Cucumber HTML | `target/cucumber-report.html` |
| Surefire / TestNG | `target/surefire-reports/` |

A successful run is **25** Cucumber scenarios, 0 failures (examples in two `Scenario Outline`s count as separate tests).

### 4. Clean rebuild

```powershell
mvn clean test
```

## Project layout

```
pom.xml
testng.xml
src/main/java/com/rebalance/model/     Account, Security, RebalanceResult
src/main/java/com/rebalance/service/   RebalancingService
src/test/java/com/rebalance/runner/    TestRunner (Cucumber + TestNG)
src/test/java/com/rebalance/stepdefinition/  RebalanceSteps
src/test/resources/features/rebalance.feature
docs/Manual_Test_Cases_Feature_Mapping.xlsx
ASSUMPTIONS.md
```

## Test coverage (feature file)

`rebalance.feature` is the specification. Glue code in `RebalanceSteps.java` maps Gherkin to `calculate` / `rebalance` and asserts actions, share counts, and `IllegalArgumentException` messages.

Covered against the assignment and the service:

- **Required output**: ABC holdings → IBM BUY 67, MSFT NONE 0, ORCL SELL 45, AAPL NONE 0, HD NONE 0
- **Single-security math**: buy, sell, already-on-target
- **Rounding**: 33.33 → 33; 2.5 → 3 (half-up); same rounding on sells; fractional target %
- **Boundaries**: target 0 (sell toward cash), target 100
- **Validation**: zero/negative asset, zero/negative price, target &gt; 100, negative target, asset checked before price
- **Account-level**: null account, blank ticker, empty holdings, all-or-nothing failure, target mix ≠ 100%, fractional targets that still total 100

Not automated (called out in [ASSUMPTIONS.md](ASSUMPTIONS.md)): current-% mix is not required to total 100; current % is not range-checked; no cash-residual / lot-size / fee model.

## Java / Maven settings in `pom.xml`

These are the runtime-relevant bits already in the POM:

```xml
<maven.compiler.source>17</maven.compiler.source>
<maven.compiler.target>17</maven.compiler.target>
<maven.compiler.release>17</maven.compiler.release>
```

Compiler plugin uses `<release>17</release>` (bytecode and bootstrap APIs for 17). Surefire is bound to `testng.xml`, so `mvn test` does not depend on IDE run configurations.

You do **not** need `--enable-preview`, module-info, or a custom `argLine`. If a future JDK deprecates 17 as a `--release` target, bump `maven.compiler.*` and the compiler plugin together; tests themselves are JDK-version-agnostic doubles and ints.
