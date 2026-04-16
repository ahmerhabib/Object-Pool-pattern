import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BenchmarkServiceTest {
    @Test
    void benchmarkReturnsMeaningfulNumbers() {
        BenchmarkService benchmarkService = new BenchmarkService();
        BenchmarkService.BenchmarkResult result = benchmarkService.run(1500, 50);

        assertTrue(result.operations >= DashboardConfig.MIN_BENCHMARK_OPERATIONS);
        assertTrue(result.pooledTotalNanos > 0);
        assertTrue(result.rawTotalNanos > 0);
        assertTrue(result.pooledAverageMicros() > 0);
        assertTrue(result.rawAverageMicros() > 0);
    }
}
