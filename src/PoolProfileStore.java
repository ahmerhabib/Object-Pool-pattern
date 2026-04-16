import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

final class PoolProfileStore {
    private static final String FILE_EXTENSION = ".properties";

    private final Path profilesDirectory;

    PoolProfileStore(Path profilesDirectory) {
        this.profilesDirectory = profilesDirectory;
    }

    void saveProfile(String profileName, DashboardConfig config) throws IOException {
        Files.createDirectories(profilesDirectory);
        String safeName = toSafeFileName(profileName);
        Path file = profilesDirectory.resolve(safeName + FILE_EXTENSION);
        Properties properties = toProperties(config);
        properties.setProperty("profileName", profileName.trim());
        try (OutputStream outputStream = Files.newOutputStream(
            file,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )) {
            properties.store(outputStream, "Object Pool Dashboard Profile");
        }
    }

    void deleteProfile(String profileName) throws IOException {
        String safeName = toSafeFileName(profileName);
        Path file = profilesDirectory.resolve(safeName + FILE_EXTENSION);
        Files.deleteIfExists(file);
    }

    Map<String, DashboardConfig> loadProfiles() throws IOException {
        Files.createDirectories(profilesDirectory);
        Map<String, DashboardConfig> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try (Stream<Path> stream = Files.list(profilesDirectory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(FILE_EXTENSION))
                .forEach(path -> {
                    try {
                        ProfileData profileData = loadProfileDataFromFile(path);
                        sorted.put(profileData.name, profileData.config);
                    } catch (IOException ignored) {
                        // Skip unreadable profile files.
                    }
                });
        }

        return new LinkedHashMap<>(sorted);
    }

    void exportConfig(Path exportFile, DashboardConfig config) throws IOException {
        Files.createDirectories(exportFile.toAbsolutePath().getParent());
        Properties properties = toProperties(config);
        try (OutputStream outputStream = Files.newOutputStream(
            exportFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )) {
            properties.store(outputStream, "Object Pool Dashboard Export");
        }
    }

    DashboardConfig importConfig(Path importFile) throws IOException {
        return loadConfigFromFile(importFile);
    }

    private DashboardConfig loadConfigFromFile(Path file) throws IOException {
        return loadProfileDataFromFile(file).config;
    }

    private ProfileData loadProfileDataFromFile(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
            properties.load(inputStream);
        }

        DashboardConfig config = new DashboardConfig(
            intProperty(properties, "initialSize", DashboardConfig.DEFAULT_INITIAL_SIZE),
            intProperty(properties, "maxSize", DashboardConfig.DEFAULT_MAX_SIZE),
            intProperty(properties, "collectFrequencyMs", DashboardConfig.DEFAULT_COLLECT_FREQUENCY_MS),
            intProperty(properties, "resetValue", DashboardConfig.DEFAULT_RESET_VALUE),
            intProperty(properties, "benchmarkOperations", DashboardConfig.DEFAULT_BENCHMARK_OPERATIONS),
            ThemeMode.valueOf(properties.getProperty("themeMode", ThemeMode.LIGHT.name())),
            intProperty(properties, "fontScalePercent", DashboardConfig.DEFAULT_FONT_SCALE_PERCENT),
            Boolean.parseBoolean(properties.getProperty("highContrast", "false"))
        );

        String name = properties.getProperty("profileName");
        if (name == null || name.trim().isEmpty()) {
            name = profileNameFromFileName(file);
        }

        return new ProfileData(name.trim(), config.sanitized());
    }

    private static Properties toProperties(DashboardConfig config) {
        Properties properties = new Properties();
        properties.setProperty("initialSize", Integer.toString(config.initialSize));
        properties.setProperty("maxSize", Integer.toString(config.maxSize));
        properties.setProperty("collectFrequencyMs", Integer.toString(config.collectFrequencyMs));
        properties.setProperty("resetValue", Integer.toString(config.resetValue));
        properties.setProperty("benchmarkOperations", Integer.toString(config.benchmarkOperations));
        properties.setProperty("themeMode", config.themeMode.name());
        properties.setProperty("fontScalePercent", Integer.toString(config.fontScalePercent));
        properties.setProperty("highContrast", Boolean.toString(config.highContrast));
        return properties;
    }

    private static String profileNameFromFileName(Path file) {
        String rawName = file.getFileName().toString();
        if (rawName.endsWith(FILE_EXTENSION)) {
            rawName = rawName.substring(0, rawName.length() - FILE_EXTENSION.length());
        }
        return rawName.isEmpty() ? "profile" : rawName;
    }

    private static String toSafeFileName(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) {
            return "profile";
        }
        String safe = trimmed
            .replaceAll("[^a-zA-Z0-9._-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-", "")
            .replaceAll("-$", "");
        return safe.isEmpty() ? "profile" : safe;
    }

    private static int intProperty(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static final class ProfileData {
        private final String name;
        private final DashboardConfig config;

        private ProfileData(String name, DashboardConfig config) {
            this.name = name;
            this.config = config;
        }
    }
}
