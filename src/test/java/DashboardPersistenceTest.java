import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DashboardPersistenceTest {
    @Test
    void writesAuditErrorSessionAndMetrics() throws IOException {
        Path tempDir = Files.createTempDirectory("dashboard-persistence-test");
        DashboardPersistence persistence = new DashboardPersistence(tempDir);
        persistence.init();

        persistence.logEvent("action.test", "message", Map.of("k", "v"));
        persistence.logError("action.error", new IllegalStateException("boom"));
        persistence.appendSessionEvent("session-line");
        persistence.appendUtilizationPoint(3, 7, 30.5);

        String auditLog = Files.readString(persistence.getAuditLogFile(), StandardCharsets.UTF_8);
        String errorLog = Files.readString(persistence.getErrorLogFile(), StandardCharsets.UTF_8);
        String sessionLog = Files.readString(persistence.getSessionHistoryFile(), StandardCharsets.UTF_8);
        String csv = Files.readString(persistence.getUtilizationCsvFile(), StandardCharsets.UTF_8);

        assertTrue(auditLog.contains("action.test"));
        assertTrue(errorLog.contains("boom"));
        assertTrue(sessionLog.contains("session-line"));
        assertTrue(csv.contains("30.5000"));
    }
}
