package com.cnpc.promoretail;

import io.zonky.test.db.postgres.embedded.DefaultPostgresBinaryResolver;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.PgBinaryResolver;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DesktopEmbeddedPostgres {

    static final String DATA_DIRECTORY_PROPERTY = "cnpc.data-directory";
    private static final String DESKTOP_PROPERTY = "cnpc.desktop";
    private static final String JPACKAGE_APP_PATH_PROPERTY = "jpackage.app-path";
    private static final String POSTGRES_PROFILE = "postgres";
    private static final String POSTGRES_ARCHIVE_NAME = "postgres-windows-x86_64.txz";
    private static final List<String> WINDOWS_RUNTIME_LIBRARIES = List.of(
            "msvcp140.dll",
            "vcruntime140.dll",
            "vcruntime140_1.dll"
    );
    private static EmbeddedPostgres database;

    private DesktopEmbeddedPostgres() {
    }

    static synchronized void configure() {
        if (!Boolean.getBoolean(DESKTOP_PROPERTY) || database != null) {
            return;
        }

        Path postgresDirectory = applicationDataDirectory().resolve("postgresql");
        Path dataDirectory = postgresDirectory.resolve("data");
        Path binaryDirectory = postgresDirectory.resolve("binaries");
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(binaryDirectory);
            PgBinaryResolver binaryResolver = postgresBinaryResolver();
            installWindowsRuntimeLibraries(binaryDirectory, binaryResolver);
            database = EmbeddedPostgres.builder()
                    .setDataDirectory(dataDirectory)
                    .setCleanDataDirectory(false)
                    .setRegisterShutdownHook(true)
                    .setOverrideWorkingDirectory(binaryDirectory.toFile())
                    .setPgBinaryResolver(binaryResolver)
                    .setServerConfig("listen_addresses", "127.0.0.1")
                    .setServerConfig("max_connections", "50")
                    .start();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize the bundled PostgreSQL database", exception);
        }

        activatePostgresProfile();
        System.setProperty("spring.datasource.url", database.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        System.setProperty("spring.flyway.enabled", "true");
    }

    private static void installWindowsRuntimeLibraries(
            Path binaryDirectory,
            PgBinaryResolver binaryResolver
    ) throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return;
        }

        Path javaBinaryDirectory = Path.of(System.getProperty("java.home"), "bin");
        Path postgresBinaryDirectory = preparePostgresBinaries(binaryDirectory, binaryResolver).resolve("bin");
        for (String libraryName : WINDOWS_RUNTIME_LIBRARIES) {
            Path source = javaBinaryDirectory.resolve(libraryName);
            if (Files.notExists(source)) {
                throw new IOException("Bundled Java runtime is missing " + libraryName);
            }
            Files.copy(source, postgresBinaryDirectory.resolve(libraryName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static PgBinaryResolver postgresBinaryResolver() {
        Path packagedArchive = packagedPostgresArchive();
        if (packagedArchive == null) {
            return DefaultPostgresBinaryResolver.INSTANCE;
        }
        return (system, architecture) -> Files.newInputStream(packagedArchive);
    }

    private static Path packagedPostgresArchive() {
        try {
            String launcherPath = System.getProperty(JPACKAGE_APP_PATH_PROPERTY);
            if (launcherPath != null && !launcherPath.isBlank()) {
                Path launcher = Path.of(launcherPath).toAbsolutePath();
                Path archive = launcher.getParent().resolve("app").resolve(POSTGRES_ARCHIVE_NAME);
                if (Files.isRegularFile(archive)) {
                    return archive;
                }
            }

            Path applicationEntry = applicationEntry();
            Path applicationDirectory = Files.isDirectory(applicationEntry)
                    ? applicationEntry
                    : applicationEntry.getParent();
            if (applicationDirectory == null) {
                return null;
            }
            Path archive = applicationDirectory.resolve(POSTGRES_ARCHIVE_NAME);
            return Files.isRegularFile(archive) ? archive : null;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Unable to locate the bundled PostgreSQL archive", exception);
        }
    }

    private static Path applicationEntry() throws URISyntaxException {
        var location = DesktopEmbeddedPostgres.class.getProtectionDomain().getCodeSource().getLocation();
        if ("file".equalsIgnoreCase(location.getProtocol())) {
            return Path.of(location.toURI());
        }

        String externalLocation = URLDecoder.decode(location.toExternalForm(), StandardCharsets.UTF_8);
        if (externalLocation.startsWith("nested:")) {
            String nestedPath = externalLocation.substring("nested:".length());
            int nestedMarker = nestedPath.indexOf("/!");
            if (nestedMarker >= 0) {
                nestedPath = nestedPath.substring(0, nestedMarker);
            }
            if (nestedPath.matches("^/[A-Za-z]:/.*")) {
                nestedPath = nestedPath.substring(1);
            }
            return Path.of(nestedPath);
        }
        if (externalLocation.startsWith("jar:file:")) {
            int nestedMarker = externalLocation.indexOf("!/");
            String jarLocation = nestedMarker >= 0
                    ? externalLocation.substring("jar:".length(), nestedMarker)
                    : externalLocation.substring("jar:".length());
            return Path.of(new java.net.URI(jarLocation));
        }
        throw new IllegalStateException("Unsupported application location: " + externalLocation);
    }

    private static Path preparePostgresBinaries(
            Path binaryDirectory,
            PgBinaryResolver binaryResolver
    ) throws IOException {
        try {
            Method prepareBinaries = EmbeddedPostgres.class.getDeclaredMethod(
                    "prepareBinaries",
                    PgBinaryResolver.class,
                    File.class
            );
            prepareBinaries.setAccessible(true);
            File postgresDirectory = (File) prepareBinaries.invoke(
                    null,
                    binaryResolver,
                    binaryDirectory.toFile()
            );
            return postgresDirectory.toPath();
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IOException("Unable to prepare the bundled PostgreSQL binaries", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException("Unable to extract the bundled PostgreSQL binaries", cause);
        }
    }

    static synchronized void stop() {
        if (database == null) {
            return;
        }
        try {
            database.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to stop the bundled PostgreSQL database", exception);
        } finally {
            database = null;
        }
    }

    private static void activatePostgresProfile() {
        Set<String> profiles = new LinkedHashSet<>();
        String configuredProfiles = System.getProperty("spring.profiles.active");
        if (configuredProfiles == null || configuredProfiles.isBlank()) {
            configuredProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        if (configuredProfiles != null && !configuredProfiles.isBlank()) {
            Arrays.stream(configuredProfiles.split(","))
                    .map(String::trim)
                    .filter(profile -> !profile.isEmpty())
                    .forEach(profiles::add);
        }
        profiles.add(POSTGRES_PROFILE);
        System.setProperty("spring.profiles.active", String.join(",", profiles));
    }

    static Path applicationDataDirectory() {
        String configuredDirectory = System.getProperty(DATA_DIRECTORY_PROPERTY);
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory);
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "CNPCSmartRetail");
        }
        return Path.of(System.getProperty("user.home"), "CNPCSmartRetail");
    }
}
