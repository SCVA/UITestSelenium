package udistrital.uitestselenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.WebDriverManagerException;
import java.io.IOException;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
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
    private static Server server;
    private static Path scratchDirectory;

    @BeforeAll
    static void setUpSuite() throws Exception {
        baseUrl = "http://localhost:8080/UITestSelenium";

        try {
            server = startJetty();
        } catch (BindException ex) {
            // When the preferred port is already in use we assume an external
            // container (e.g. XAMPP) is exposing the application locally.
            server = null;
        }

        waitForApplication();

        try {
            WebDriverManager.chromedriver().setup();
        } catch (WebDriverManagerException ex) {
            // Fallback to Selenium Manager when WebDriverManager cannot reach
            // the driver repository (e.g. behind a proxy).
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
    }

    @AfterAll
    static void tearDownSuite() {
        if (driver != null) {
            driver.quit();
        }
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ignored) {
                // Best effort to stop Jetty between tests.
            }
        }
        if (scratchDirectory != null) {
            try (java.util.stream.Stream<Path> paths = Files.walk(scratchDirectory)) {
                paths.sorted((left, right) -> right.compareTo(left))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // Ignore deletion issues during cleanup.
                            }
                        });
            } catch (IOException ignored) {
                // Ignore cleanup issues.
            }
        }
    }

    private static Server startJetty() throws Exception {
        Server localServer = new Server(8080);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/UITestSelenium");
        webAppContext.setResourceBase(resolveWebAppResourceBase());
        webAppContext.setParentLoaderPriority(true);
        webAppContext.setWelcomeFiles(new String[] {"index.jsp"});
        webAppContext.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*taglibs.*\\.jar$|.*/[^/]*jsp.*\\.jar$");

        scratchDirectory = Files.createTempDirectory("jetty-jsp");
        webAppContext.setTempDirectory(scratchDirectory.toFile());
        webAppContext.setAttribute("javax.servlet.context.tempdir", scratchDirectory);

        localServer.setHandler(webAppContext);
        localServer.start();

        return localServer;
    }

    private static String resolveWebAppResourceBase() throws IOException {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path webAppPath = projectRoot.resolve(Paths.get("src", "main", "webapp"));
        if (!Files.exists(webAppPath)) {
            throw new IOException("Could not locate web application resources at " + webAppPath);
        }
        return webAppPath.toString();
    }

    private static void waitForApplication() throws InterruptedException, IOException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl).openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                connection.setInstanceFollowRedirects(false);
                int status = connection.getResponseCode();
                connection.disconnect();
                if (status >= 200 && status < 500) {
                    return;
                }
            } catch (IOException ex) {
                lastFailure = ex;
            }
            Thread.sleep(1000);
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new IOException("Application at " + baseUrl + " did not become available in time");
    }

    @Test
    @DisplayName("Successful credentials navigate to the welcome page")
    void userCanAuthenticateWithKnownCredentials() {
        driver.get(baseUrl);

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

        wait.until(ExpectedConditions.urlContains("/login"));
        String headingText = welcomeHeading.getText();
        org.junit.jupiter.api.Assertions.assertTrue(headingText.contains("user@example.com"),
                "Expected heading to contain the authenticated email, but was: " + headingText);
    }

}
