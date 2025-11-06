package udistrital.uitestselenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
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
import org.eclipse.jetty.server.ServerConnector;
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
        baseUrl = startJetty();
        waitForServerReadiness(baseUrl);

        String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromeDriverPath != null && !chromeDriverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        } else {
            WebDriverManager.chromedriver().setup();
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--proxy-server=direct://",
                "--proxy-bypass-list=*",
                "--remote-allow-origins=*",
                "--window-size=1920,1080",
                "--allow-insecure-localhost");
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

    private static String startJetty() throws Exception {
        server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/UITestSelenium");

        Path webAppDir = Paths.get("src", "main", "webapp").toAbsolutePath();
        context.setResourceBase(webAppDir.toString());
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        context.setWelcomeFiles(new String[]{"index.jsp"});
        context.setParentLoaderPriority(false);

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

        ClassLoader jspClassLoader = new URLClassLoader(new URL[0],
                LoginFlowIT.class.getClassLoader());
        context.setClassLoader(jspClassLoader);

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
        context.addServlet(jsp, "*.jsp");

        ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
        defaultServlet.setInitParameter("dirAllowed", "false");
        context.addServlet(defaultServlet, "/");

        server.setHandler(context);
        server.start();

        int port = connector.getLocalPort();
        if (port <= 0) {
            throw new IllegalStateException("Unable to determine Jetty server port");
        }
        return "http://127.0.0.1:" + port + "/UITestSelenium";
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

    private static void waitForServerReadiness(String serverBaseUrl) throws InterruptedException {
        URL healthUrl;
        try {
            healthUrl = new URL(serverBaseUrl + "/index.jsp");
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to build Jetty health-check URL", ex);
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

        StringBuilder message = new StringBuilder("Embedded Jetty readiness probe failed to detect the login page");
        if (lastStatus != null) {
            message.append(". Last HTTP status: ").append(lastStatus);
        }
        if (lastException != null) {
            message.append(". Last error: ").append(lastException.getMessage());
        }
        if (lastBodySnippet != null) {
            message.append("\nLast response body snippet:\n").append(lastBodySnippet);
        }

        throw new IllegalStateException(message.toString(), lastException);
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
