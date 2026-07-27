package com.cnpc.promoretail;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs(OS.WINDOWS)
class DesktopEmbeddedPostgresTest {

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void configureDesktopDataDirectory() {
        System.setProperty("cnpc.desktop", "true");
        System.setProperty(DesktopEmbeddedPostgres.DATA_DIRECTORY_PROPERTY, temporaryDirectory.toString());
        System.clearProperty("spring.profiles.active");
    }

    @AfterEach
    void stopDatabase() {
        DesktopEmbeddedPostgres.stop();
        System.clearProperty("cnpc.desktop");
        System.clearProperty(DesktopEmbeddedPostgres.DATA_DIRECTORY_PROPERTY);
        System.clearProperty("spring.profiles.active");
        System.clearProperty("spring.datasource.url");
        System.clearProperty("spring.datasource.username");
        System.clearProperty("spring.datasource.password");
        System.clearProperty("spring.flyway.enabled");
        System.clearProperty("jpackage.app-path");
    }

    @Test
    void locatesPostgresArchiveBesidePackagedApplication() throws Exception {
        Path launcher = temporaryDirectory.resolve("installed/CNPC Smart Retail.exe");
        Path archive = launcher.getParent().resolve("app/postgres-windows-x86_64.txz");
        Files.createDirectories(archive.getParent());
        Files.writeString(launcher, "launcher");
        Files.writeString(archive, "postgres archive");
        System.setProperty("jpackage.app-path", launcher.toString());

        var method = DesktopEmbeddedPostgres.class.getDeclaredMethod("packagedPostgresArchive");
        method.setAccessible(true);
        assertThat(method.invoke(null)).isEqualTo(archive);
    }

    @Test
    void startsPostgresAndReusesItsDataAfterRestart() throws Exception {
        DesktopEmbeddedPostgres.configure();
        String firstJdbcUrl = System.getProperty("spring.datasource.url");

        try (var connection = DriverManager.getConnection(firstJdbcUrl, "postgres", "postgres");
             var statement = connection.createStatement()) {
            statement.execute("create table desktop_persistence_check (value varchar(40) not null)");
            statement.execute("insert into desktop_persistence_check values ('kept-after-restart')");
        }
        DesktopEmbeddedPostgres.stop();

        DesktopEmbeddedPostgres.configure();
        String secondJdbcUrl = System.getProperty("spring.datasource.url");
        try (var connection = DriverManager.getConnection(secondJdbcUrl, "postgres", "postgres");
             var statement = connection.createStatement();
             var result = statement.executeQuery("select value from desktop_persistence_check")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("kept-after-restart");
        }

        assertThat(System.getProperty("spring.profiles.active")).contains("postgres");
        assertThat(temporaryDirectory.resolve("postgresql/data/PG_VERSION")).exists();
        try (var files = Files.walk(temporaryDirectory.resolve("postgresql/binaries"))) {
            assertThat(files
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("vcruntime140.dll")))
                    .singleElement()
                    .satisfies(path -> assertThat(path).exists());
        }
    }
}
