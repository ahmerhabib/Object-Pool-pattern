import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DashboardConfigTest {
    @Test
    void defaultConfigIsValid() {
        ValidationResult result = DashboardConfig.defaults().validate();
        assertTrue(result.valid);
    }

    @Test
    void rejectsCollectorFrequencyBelowSafeThreshold() {
        DashboardConfig config = new DashboardConfig(1, 10, 50, 0, 20000, ThemeMode.LIGHT, 100, false);
        ValidationResult result = config.validate();

        assertFalse(result.valid);
    }

    @Test
    void sanitizationClampsOutOfRangeValues() {
        DashboardConfig raw = new DashboardConfig(50000, -2, 7, 0, 9999999, ThemeMode.DARK, 25, true);
        DashboardConfig sanitized = raw.sanitized();

        assertTrue(sanitized.validate().valid);
    }
}
