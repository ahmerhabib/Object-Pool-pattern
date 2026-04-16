import java.util.Objects;

final class DashboardConfig {
    static final int DEFAULT_INITIAL_SIZE = 3;
    static final int DEFAULT_MAX_SIZE = 24;
    static final int DEFAULT_COLLECT_FREQUENCY_MS = -1;
    static final int DEFAULT_RESET_VALUE = 0;
    static final int DEFAULT_BENCHMARK_OPERATIONS = 20000;
    static final int DEFAULT_FONT_SCALE_PERCENT = 100;

    static final int MIN_INITIAL_SIZE = 0;
    static final int MAX_INITIAL_SIZE = 1000;

    static final int MIN_MAX_SIZE = 1;
    static final int MAX_MAX_SIZE = 10000;

    static final int MIN_COLLECT_FREQUENCY_MS = 100;
    static final int MAX_COLLECT_FREQUENCY_MS = 120000;

    static final int MIN_FONT_SCALE_PERCENT = 80;
    static final int MAX_FONT_SCALE_PERCENT = 170;

    static final int MIN_BENCHMARK_OPERATIONS = 500;
    static final int MAX_BENCHMARK_OPERATIONS = 500000;

    final int initialSize;
    final int maxSize;
    final int collectFrequencyMs;
    final int resetValue;
    final int benchmarkOperations;
    final ThemeMode themeMode;
    final int fontScalePercent;
    final boolean highContrast;

    DashboardConfig(
        int initialSize,
        int maxSize,
        int collectFrequencyMs,
        int resetValue,
        int benchmarkOperations,
        ThemeMode themeMode,
        int fontScalePercent,
        boolean highContrast
    ) {
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.collectFrequencyMs = collectFrequencyMs;
        this.resetValue = resetValue;
        this.benchmarkOperations = benchmarkOperations;
        this.themeMode = Objects.requireNonNull(themeMode, "themeMode cannot be null");
        this.fontScalePercent = fontScalePercent;
        this.highContrast = highContrast;
    }

    static DashboardConfig defaults() {
        return new DashboardConfig(
            DEFAULT_INITIAL_SIZE,
            DEFAULT_MAX_SIZE,
            DEFAULT_COLLECT_FREQUENCY_MS,
            DEFAULT_RESET_VALUE,
            DEFAULT_BENCHMARK_OPERATIONS,
            ThemeMode.LIGHT,
            DEFAULT_FONT_SCALE_PERCENT,
            false
        );
    }

    ValidationResult validate() {
        if (initialSize < MIN_INITIAL_SIZE || initialSize > MAX_INITIAL_SIZE) {
            return ValidationResult.invalid("Initial Size must be between " + MIN_INITIAL_SIZE + " and " + MAX_INITIAL_SIZE + ".");
        }
        if (maxSize < MIN_MAX_SIZE || maxSize > MAX_MAX_SIZE) {
            return ValidationResult.invalid("Max Size must be between " + MIN_MAX_SIZE + " and " + MAX_MAX_SIZE + ".");
        }
        if (initialSize > maxSize) {
            return ValidationResult.invalid("Initial Size cannot be greater than Max Size.");
        }
        if (collectFrequencyMs != -1 && (collectFrequencyMs < MIN_COLLECT_FREQUENCY_MS || collectFrequencyMs > MAX_COLLECT_FREQUENCY_MS)) {
            return ValidationResult.invalid(
                "Collector must be -1 or between " + MIN_COLLECT_FREQUENCY_MS + " and " + MAX_COLLECT_FREQUENCY_MS + " ms."
            );
        }
        if (fontScalePercent < MIN_FONT_SCALE_PERCENT || fontScalePercent > MAX_FONT_SCALE_PERCENT) {
            return ValidationResult.invalid(
                "Font scale must be between " + MIN_FONT_SCALE_PERCENT + "% and " + MAX_FONT_SCALE_PERCENT + "%."
            );
        }
        if (benchmarkOperations < MIN_BENCHMARK_OPERATIONS || benchmarkOperations > MAX_BENCHMARK_OPERATIONS) {
            return ValidationResult.invalid(
                "Benchmark operations must be between " + MIN_BENCHMARK_OPERATIONS + " and " + MAX_BENCHMARK_OPERATIONS + "."
            );
        }
        return ValidationResult.valid();
    }

    DashboardConfig sanitized() {
        int sanitizedInitial = clamp(initialSize, MIN_INITIAL_SIZE, MAX_INITIAL_SIZE);
        int sanitizedMax = clamp(maxSize, MIN_MAX_SIZE, MAX_MAX_SIZE);
        if (sanitizedInitial > sanitizedMax) {
            sanitizedInitial = sanitizedMax;
        }

        int sanitizedCollect = collectFrequencyMs;
        if (sanitizedCollect != -1) {
            sanitizedCollect = clamp(sanitizedCollect, MIN_COLLECT_FREQUENCY_MS, MAX_COLLECT_FREQUENCY_MS);
        }

        int sanitizedFontScale = clamp(fontScalePercent, MIN_FONT_SCALE_PERCENT, MAX_FONT_SCALE_PERCENT);
        int sanitizedBenchmark = clamp(benchmarkOperations, MIN_BENCHMARK_OPERATIONS, MAX_BENCHMARK_OPERATIONS);

        return new DashboardConfig(
            sanitizedInitial,
            sanitizedMax,
            sanitizedCollect,
            resetValue,
            sanitizedBenchmark,
            themeMode,
            sanitizedFontScale,
            highContrast
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DashboardConfig)) {
            return false;
        }
        DashboardConfig that = (DashboardConfig) other;
        return initialSize == that.initialSize
            && maxSize == that.maxSize
            && collectFrequencyMs == that.collectFrequencyMs
            && resetValue == that.resetValue
            && benchmarkOperations == that.benchmarkOperations
            && fontScalePercent == that.fontScalePercent
            && highContrast == that.highContrast
            && themeMode == that.themeMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            initialSize,
            maxSize,
            collectFrequencyMs,
            resetValue,
            benchmarkOperations,
            themeMode,
            fontScalePercent,
            highContrast
        );
    }
}
