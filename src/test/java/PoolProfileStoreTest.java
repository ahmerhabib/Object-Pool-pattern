import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PoolProfileStoreTest {
    @Test
    void savesLoadsAndDeletesProfiles() throws IOException {
        Path tempDir = Files.createTempDirectory("pool-profile-store-test");
        PoolProfileStore store = new PoolProfileStore(tempDir);
        DashboardConfig config = new DashboardConfig(2, 30, 1000, 8, 25000, ThemeMode.DARK, 110, true);

        store.saveProfile("QA Profile", config);
        Map<String, DashboardConfig> loaded = store.loadProfiles();

        assertTrue(loaded.containsKey("QA Profile"));
        assertEquals(config, loaded.get("QA Profile"));

        store.deleteProfile("QA Profile");
        Map<String, DashboardConfig> reloaded = store.loadProfiles();
        assertTrue(reloaded.isEmpty());
    }

    @Test
    void exportsAndImportsConfig() throws IOException {
        Path tempDir = Files.createTempDirectory("pool-profile-export-test");
        PoolProfileStore store = new PoolProfileStore(tempDir.resolve("profiles"));

        DashboardConfig expected = new DashboardConfig(5, 40, -1, 4, 15000, ThemeMode.LIGHT, 100, false);
        Path exportFile = tempDir.resolve("settings.properties");

        store.exportConfig(exportFile, expected);
        DashboardConfig imported = store.importConfig(exportFile);

        assertEquals(expected, imported);
    }
}
