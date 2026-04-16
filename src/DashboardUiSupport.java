import java.util.List;
import java.util.Locale;

final class DashboardUiSupport {
    private DashboardUiSupport() {
    }

    static boolean matchesFilter(String searchable, String filterText) {
        if (filterText == null || filterText.trim().isEmpty()) {
            return true;
        }
        if (searchable == null) {
            return false;
        }
        return searchable.toLowerCase(Locale.ROOT).contains(filterText.trim().toLowerCase(Locale.ROOT));
    }

    static String buildDetailText(
        int id,
        int value,
        String state,
        int borrowCount,
        String createdAt,
        String lastUsedAt
    ) {
        return "Item Details\n"
            + "------------\n"
            + "ID: " + id + "\n"
            + "Value: " + value + "\n"
            + "State: " + state + "\n"
            + "Borrow Count: " + borrowCount + "\n"
            + "Created At: " + createdAt + "\n"
            + "Last Used At: " + lastUsedAt + "\n";
    }

    static String utilizationSparkline(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return "No utilization samples yet.";
        }
        String ramp = "▁▂▃▄▅▆▇█";
        StringBuilder sparkline = new StringBuilder();
        for (Double point : data) {
            double value = point == null ? 0 : Math.max(0.0, Math.min(100.0, point));
            int index = (int) Math.round((value / 100.0) * (ramp.length() - 1));
            sparkline.append(ramp.charAt(index));
        }
        return sparkline.toString();
    }
}
