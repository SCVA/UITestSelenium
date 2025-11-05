package udistrital.uitestselenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class LoginFlowIT {

    private static WebDriver driver;
    private static String baseUrl;

    @BeforeAll
    static void setUpSuite() {
        baseUrl = "http://localhost:8080/UITestSelenium";

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
    }

    @AfterAll
    static void tearDownSuite() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Successful credentials navigate to the welcome page")
    void userCanAuthenticateWithKnownCredentials() {
        driver.get(baseUrl + "/index.jsp");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        WebElement emailField = wait
                .until(ExpectedConditions.elementToBeClickable(By.name("email")));
        emailField.clear();
        emailField.sendKeys("user@example.com");

        WebElement passwordField = wait
                .until(ExpectedConditions.elementToBeClickable(By.name("password")));
        passwordField.clear();
        passwordField.sendKeys("password123");

        passwordField.submit();

        WebElement welcomeHeading = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".welcome-card h1")));

        wait.until(ExpectedConditions.urlContains("/welcome.jsp"));
        String headingText = welcomeHeading.getText();
        org.junit.jupiter.api.Assertions.assertTrue(headingText.contains("user@example.com"),
                "Expected heading to contain the authenticated email, but was: " + headingText);
    }

}
