import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

final class DashboardPersistence {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Path logsDirectory;
    private final Path historyDirectory;
    private final Path auditLogFile;
    private final Path errorLogFile;
    private final Path sessionHistoryFile;
    private final Path utilizationCsvFile;

    DashboardPersistence(Path baseDirectory) {
        this.logsDirectory = baseDirectory.resolve("logs");
        this.historyDirectory = baseDirectory.resolve("history");
        this.auditLogFile = logsDirectory.resolve("audit-log.jsonl");
        this.errorLogFile = logsDirectory.resolve("errors.jsonl");
        this.sessionHistoryFile = historyDirectory.resolve("session-history.log");
        this.utilizationCsvFile = historyDirectory.resolve("utilization-trend.csv");
    }

    void init() {
        try {
            Files.createDirectories(logsDirectory);
            Files.createDirectories(historyDirectory);
            if (Files.notExists(utilizationCsvFile)) {
                appendLine(utilizationCsvFile, "timestamp,active,available,utilization\n");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize dashboard persistence", e);
        }
    }

    void logEvent(String action, String message, Map<String, String> fields) {
        StringBuilder line = new StringBuilder();
        line.append("{")
            .append(jsonPair("timestamp", LocalDateTime.now().format(TIME_FORMAT))).append(",")
            .append(jsonPair("action", action)).append(",")
            .append(jsonPair("message", message));

        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                line.append(",").append(jsonPair(entry.getKey(), entry.getValue()));
            }
        }

        line.append("}\n");
        appendLine(auditLogFile, line.toString());
    }

    void logError(String action, Throwable throwable) {
        String errorMessage = throwable == null ? "unknown" : String.valueOf(throwable.getMessage());
        String throwableType = throwable == null ? "unknown" : throwable.getClass().getName();
        String line = "{" +
            jsonPair("timestamp", LocalDateTime.now().format(TIME_FORMAT)) + "," +
            jsonPair("action", action) + "," +
            jsonPair("errorType", throwableType) + "," +
            jsonPair("message", errorMessage) +
            "}\n";
        appendLine(errorLogFile, line);
    }

    void appendSessionEvent(String line) {
        String entry = LocalDateTime.now().format(TIME_FORMAT) + " " + line + "\n";
        appendLine(sessionHistoryFile, entry);
    }

    void appendUtilizationPoint(int active, int available, double utilization) {
        String csv = String.format(
            "%s,%d,%d,%.4f%n",
            LocalDateTime.now().format(TIME_FORMAT),
            active,
            available,
            utilization
        );
        appendLine(utilizationCsvFile, csv);
    }

    Path getAuditLogFile() {
        return auditLogFile;
    }

    Path getErrorLogFile() {
        return errorLogFile;
    }

    Path getSessionHistoryFile() {
        return sessionHistoryFile;
    }

    Path getUtilizationCsvFile() {
        return utilizationCsvFile;
    }

    private void appendLine(Path file, String line) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(
                file,
                line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write to " + file, e);
        }
    }

    private static String jsonPair(String key, String value) {
        return quote(key) + ":" + quote(value == null ? "" : value);
    }

    private static String quote(String value) {
        String escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }
}
