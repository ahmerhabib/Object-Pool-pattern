import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DashboardUiSupportTest {
    @Test
    void filterMatchingIsCaseInsensitive() {
        assertTrue(DashboardUiSupport.matchesFilter("Item#12 value=3 state=active", "ACTIVE"));
        assertFalse(DashboardUiSupport.matchesFilter("Item#12 value=3 state=active", "paused"));
    }

    @Test
    void detailTextContainsImportantFields() {
        String details = DashboardUiSupport.buildDetailText(9, 22, "active", 4, "10:00:00", "10:01:00");

        assertTrue(details.contains("ID: 9"));
        assertTrue(details.contains("Borrow Count: 4"));
    }

    @Test
    void sparklineIsRenderedForSamples() {
        String sparkline = DashboardUiSupport.utilizationSparkline(List.of(0.0, 25.0, 50.0, 75.0, 100.0));
        assertTrue(sparkline.length() == 5);
    }
}
