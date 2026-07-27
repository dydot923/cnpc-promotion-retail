package com.cnpc.promoretail;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

final class DesktopApplicationLifecycle {

    private static final String DESKTOP_PROPERTY = "cnpc.desktop";
    private static final int DEFAULT_PORT = 18083;
    private static final String APP_PATH = "/operation-campaigns";
    private static TrayIcon trayIcon;

    private DesktopApplicationLifecycle() {
    }

    static boolean openRunningInstance() {
        if (!isDesktopMode()) {
            return false;
        }
        configureLogDirectory();
        if (!isApplicationReady()) {
            return false;
        }
        openBrowser();
        return true;
    }

    static void start(ConfigurableApplicationContext context) {
        if (!isDesktopMode()) {
            return;
        }
        installTrayIcon(context);
        openBrowser();
    }

    private static boolean isDesktopMode() {
        return Boolean.getBoolean(DESKTOP_PROPERTY);
    }

    private static void configureLogDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path logDirectory = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), "CNPCSmartRetail", "logs")
                : Path.of(localAppData, "CNPCSmartRetail", "logs");
        try {
            Files.createDirectories(logDirectory);
            System.setProperty("LOG_PATH", logDirectory.toString());
        } catch (IOException ignored) {
            // The console appender remains available when the log directory cannot be created.
        }
    }

    private static boolean isApplicationReady() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/checkout/capabilities"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200
                    && response.body().contains("cnpc-promotion-retail")
                    && response.body().contains("checkout-v2");
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void installTrayIcon(ConfigurableApplicationContext context) {
        if (!SystemTray.isSupported()) {
            return;
        }

        PopupMenu menu = new PopupMenu();
        MenuItem openItem = new MenuItem("Open CNPC Smart Retail");
        openItem.addActionListener(event -> openBrowser());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(event -> shutdown(context));
        menu.add(openItem);
        menu.addSeparator();
        menu.add(exitItem);

        trayIcon = new TrayIcon(createTrayImage(), "CNPC Smart Retail", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> openBrowser());
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ignored) {
            trayIcon = null;
        }
    }

    private static BufferedImage createTrayImage() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(198, 24, 36));
        graphics.fillRoundRect(1, 1, 30, 30, 6, 6);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        graphics.drawString("CN", 5, 21);
        graphics.dispose();
        return image;
    }

    private static void openBrowser() {
        URI uri = URI.create(baseUrl() + APP_PATH);
        try {
            ProcessBuilder browserLauncher = new ProcessBuilder(
                    "rundll32", "url.dll,FileProtocolHandler", uri.toString());
            String windowsDirectory = System.getenv("WINDIR");
            if (windowsDirectory != null && !windowsDirectory.isBlank()) {
                browserLauncher.directory(Path.of(windowsDirectory, "System32").toFile());
            }
            browserLauncher.start();
        } catch (IOException ignored) {
            // The service remains available at the local URL even if no browser can be opened.
        }
    }

    private static void shutdown(ConfigurableApplicationContext context) {
        Thread shutdownThread = new Thread(() -> {
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
            SpringApplication.exit(context);
            System.exit(0);
        }, "cnpc-desktop-shutdown");
        shutdownThread.setDaemon(false);
        shutdownThread.start();
    }

    private static String baseUrl() {
        return "http://127.0.0.1:" + Integer.getInteger("server.port", DEFAULT_PORT);
    }
}
