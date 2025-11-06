package udistrital.uitestselenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
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
        startJetty();

        baseUrl = "http://localhost:8080/UITestSelenium";

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

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement emailField = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.name("email")));
        emailField.clear();
        emailField.sendKeys("user@example.com");

        WebElement passwordField = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.name("password")));
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
