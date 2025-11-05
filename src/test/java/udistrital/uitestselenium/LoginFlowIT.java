package udistrital.uitestselenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
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

    private static Server server;
    private static WebDriver driver;
    private static String baseUrl;

    @BeforeAll
    static void setUpSuite() throws Exception {
        server = createServer();
        server.start();
        baseUrl = "http://localhost:" + determinePort(server);

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
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
    }

    @Test
    @DisplayName("Successful credentials navigate to the welcome page")
    void userCanAuthenticateWithKnownCredentials() {
        driver.get(baseUrl + "/index.jsp");

        WebElement emailField = driver.findElement(By.name("email"));
        emailField.clear();
        emailField.sendKeys("user@example.com");

        WebElement passwordField = driver.findElement(By.name("password"));
        passwordField.clear();
        passwordField.sendKeys("password123");

        passwordField.submit();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement welcomeHeading = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".welcome-card h1")));

        wait.until(ExpectedConditions.urlContains("/welcome.jsp"));
        String headingText = welcomeHeading.getText();
        org.junit.jupiter.api.Assertions.assertTrue(headingText.contains("user@example.com"),
                "Expected heading to contain the authenticated email, but was: " + headingText);
    }

    private static Server createServer() throws IOException {
        Server embeddedServer = new Server(new InetSocketAddress("localhost", 0));

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        context.setResourceBase(resolveWebAppDirectory());
        context.addServlet(new ServletHolder(new LoginServlet()), "/login");
        context.addServlet(new ServletHolder("default", DefaultServlet.class), "/");

        enableEmbeddedJsp(context);

        embeddedServer.setHandler(context);
        return embeddedServer;
    }

    private static void enableEmbeddedJsp(WebAppContext context) throws IOException {
        File tempDirectory = Files.createTempDirectory("embedded-jetty-jsp").toFile();
        tempDirectory.deleteOnExit();
        context.setAttribute("javax.servlet.context.tempdir", tempDirectory);
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*taglibs.*\\.jar$|.*/javax.servlet.jsp.jstl.*\\.jar$|.*/[^/]*jsp-api-[^/]*\\.jar$|.*/[^/]*servlet-api-[^/]*\\.jar$");

        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        context.setClassLoader(jspClassLoader);

        context.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());

        ServletHolder jsp = new ServletHolder("jsp", org.eclipse.jetty.jsp.JettyJspServlet.class);
        jsp.setInitOrder(0);
        jsp.setInitParameter("logVerbosityLevel", "ERROR");
        jsp.setInitParameter("fork", "false");
        jsp.setInitParameter("xpoweredBy", "false");
        jsp.setInitParameter("compilerTargetVM", "1.8");
        jsp.setInitParameter("compilerSourceVM", "1.8");
        jsp.setInitParameter("keepgenerated", "true");
        context.addServlet(jsp, "*.jsp");
    }

    private static List<ContainerInitializer> jspInitializers() {
        org.eclipse.jetty.apache.jsp.JettyJasperInitializer initializer = new org.eclipse.jetty.apache.jsp.JettyJasperInitializer();
        return Collections.singletonList(new ContainerInitializer(initializer, null));
    }

    private static int determinePort(Server startedServer) {
        return ((ServerConnector) startedServer.getConnectors()[0]).getLocalPort();
    }

    private static String resolveWebAppDirectory() {
        Path webAppDir = Path.of("src", "main", "webapp");
        return webAppDir.toAbsolutePath().toString();
    }
}
