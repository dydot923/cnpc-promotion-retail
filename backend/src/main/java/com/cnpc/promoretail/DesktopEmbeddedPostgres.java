package com.cnpc.promoretail;

import io.zonky.test.db.postgres.embedded.DefaultPostgresBinaryResolver;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.PgBinaryResolver;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private static final String POSTGRES_PROFILE = "postgres";
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
            installWindowsRuntimeLibraries(binaryDirectory);
            database = EmbeddedPostgres.builder()
                    .setDataDirectory(dataDirectory)
                    .setCleanDataDirectory(false)
                    .setRegisterShutdownHook(true)
                    .setOverrideWorkingDirectory(binaryDirectory.toFile())
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

    private static void installWindowsRuntimeLibraries(Path binaryDirectory) throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return;
        }

        Path javaBinaryDirectory = Path.of(System.getProperty("java.home"), "bin");
        Path postgresBinaryDirectory = preparePostgresBinaries(binaryDirectory).resolve("bin");
        for (String libraryName : WINDOWS_RUNTIME_LIBRARIES) {
            Path source = javaBinaryDirectory.resolve(libraryName);
            if (Files.notExists(source)) {
                throw new IOException("Bundled Java runtime is missing " + libraryName);
            }
            Files.copy(source, postgresBinaryDirectory.resolve(libraryName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path preparePostgresBinaries(Path binaryDirectory) throws IOException {
        try {
            Method prepareBinaries = EmbeddedPostgres.class.getDeclaredMethod(
                    "prepareBinaries",
                    PgBinaryResolver.class,
                    File.class
            );
            prepareBinaries.setAccessible(true);
            File postgresDirectory = (File) prepareBinaries.invoke(
                    null,
                    DefaultPostgresBinaryResolver.INSTANCE,
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
