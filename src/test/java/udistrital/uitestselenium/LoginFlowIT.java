package udistrital.uitestselenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
    private static Path jettyTempDir;

    @BeforeAll
    static void setUpSuite() throws Exception {
        baseUrl = "http://localhost:8080/UITestSelenium";

        startJetty();
        waitForServerReadiness();

        String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromeDriverPath != null && !chromeDriverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        } else {
            WebDriverManager.chromedriver().setup();
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        String chromeBinary = System.getenv("CHROME_PATH");
        if (chromeBinary != null && !chromeBinary.isEmpty()) {
            options.setBinary(chromeBinary);
        }
        driver = new ChromeDriver(options);
    }

    @AfterAll
    static void tearDownSuite() throws Exception {
        if (driver != null) {
            driver.quit();
        }

        if (server != null) {
            server.stop();
            server.destroy();
        }

        if (jettyTempDir != null) {
            try (Stream<Path> paths = Files.walk(jettyTempDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // Swallow cleanup errors silently.
                            }
                        });
            } catch (IOException ignored) {
                // Swallow cleanup errors silently.
            }
        }
    }

    private static void startJetty() throws Exception {
        server = new Server(8080);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/UITestSelenium");

        Path webAppDir = Paths.get("src", "main", "webapp").toAbsolutePath();
        context.setResourceBase(webAppDir.toString());
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        context.setWelcomeFiles(new String[]{"index.jsp"});
        context.setParentLoaderPriority(true);

        jettyTempDir = Files.createTempDirectory("jetty-embedded-");
        context.setAttribute("javax.servlet.context.tempdir", jettyTempDir.toFile());

        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*taglibs.*\\.jar$|.*/javax\\.servlet\\.jsp\\.jstl-.*\\.jar$");

        context.setConfigurations(new Configuration[]{
            new AnnotationConfiguration(),
            new WebInfConfiguration(),
            new WebXmlConfiguration(),
            new MetaInfConfiguration(),
            new FragmentConfiguration(),
            new EnvConfiguration(),
            new PlusConfiguration(),
            new JettyWebXmlConfiguration()
        });

        context.setClassLoader(Thread.currentThread().getContextClassLoader());

        List<ContainerInitializer> initializers = new ArrayList<>();
        initializers.add(new ContainerInitializer(new JettyJasperInitializer(), null));
        context.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        context.addBean(new ServletContainerInitializersStarter(context), true);

        ServletHolder jsp = new ServletHolder("jsp", JspServlet.class);
        jsp.setInitParameter("logVerbosityLevel", "ERROR");
        jsp.setInitParameter("fork", "false");
        jsp.setInitParameter("xpoweredBy", "false");
        jsp.setInitParameter("compilerTargetVM", "1.8");
        jsp.setInitParameter("compilerSourceVM", "1.8");
        jsp.setInitParameter("keepgenerated", "false");
        jsp.setInitParameter("classpath",
                System.getProperty("java.class.path"));
        context.addServlet(jsp, "*.jsp");

        ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
        defaultServlet.setInitParameter("dirAllowed", "false");
        context.addServlet(defaultServlet, "/");

        server.setHandler(context);
        server.start();
    }

    @Test
    @DisplayName("Successful credentials navigate to the welcome page")
    void userCanAuthenticateWithKnownCredentials() {
        driver.get(baseUrl + "/index.jsp");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        wait.until(webDriver -> {
            Object readyState = ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState");
            return "complete".equals(String.valueOf(readyState));
        });

        WebElement emailField = wait
                .until(ExpectedConditions.presenceOfElementLocated(By.name("email")));
        emailField.clear();
        emailField.sendKeys("user@example.com");

        WebElement passwordField = wait
                .until(ExpectedConditions.presenceOfElementLocated(By.name("password")));
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

    private static void waitForServerReadiness() throws InterruptedException {
        URL healthUrl;
        try {
            healthUrl = new URL(baseUrl + "/index.jsp");
        } catch (IOException ex) {
            System.err.println("WARN: Unable to build Jetty health-check URL: " + ex.getMessage());
            return;
        }

        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        IOException lastException = null;
        Integer lastStatus = null;
        String lastBodySnippet = null;

        while (System.nanoTime() < deadline) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) healthUrl.openConnection();
                connection.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
                connection.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
                connection.setInstanceFollowRedirects(false);

                int responseCode = connection.getResponseCode();
                lastStatus = responseCode;

                String body = null;
                try {
                    body = readBody(connection, responseCode);
                } catch (IOException bodyReadException) {
                    lastException = bodyReadException;
                }

                if (body != null && !body.isEmpty()) {
                    lastBodySnippet = body.length() > 512 ? body.substring(0, 512) : body;
                }

                if (responseCode >= 200 && responseCode < 300
                        && body != null && body.contains("name=\"email\"")) {
                    return;
                }
            } catch (IOException ex) {
                lastException = ex;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            Thread.sleep(Duration.ofMillis(500).toMillis());
        }

        if (lastException != null) {
            System.err.println("WARN: Embedded Jetty readiness probe failed after retries: "
                    + lastException.getMessage());
        } else if (lastStatus != null) {
            System.err.println("WARN: Embedded Jetty readiness probe ended with HTTP status " + lastStatus);
        } else {
            System.err.println("WARN: Embedded Jetty readiness probe timed out without a response");
        }

        if (lastBodySnippet != null) {
            System.err.println("Last response body snippet:\n" + lastBodySnippet);
        }
    }

    private static String readBody(HttpURLConnection connection, int responseCode) throws IOException {
        try (InputStream stream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
            if (stream == null) {
                return null;
            }
            byte[] bytes = stream.readAllBytes();
            if (bytes.length == 0) {
                return null;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

}
