package com.rebalance.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.rebalance.stepdefinition",
        monochrome = true,
        plugin = {
                "pretty",
                "html:target/cucumber-report.html"
        }
)
public class TestRunner
        extends AbstractTestNGCucumberTests {
}