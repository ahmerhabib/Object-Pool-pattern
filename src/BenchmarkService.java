final class BenchmarkService {
    static final class BenchmarkResult {
        final int operations;
        final long pooledTotalNanos;
        final long rawTotalNanos;

        BenchmarkResult(int operations, long pooledTotalNanos, long rawTotalNanos) {
            this.operations = operations;
            this.pooledTotalNanos = pooledTotalNanos;
            this.rawTotalNanos = rawTotalNanos;
        }

        double pooledAverageMicros() {
            return operations == 0 ? 0 : (pooledTotalNanos / 1000.0) / operations;
        }

        double rawAverageMicros() {
            return operations == 0 ? 0 : (rawTotalNanos / 1000.0) / operations;
        }

        double speedup() {
            if (pooledTotalNanos == 0) {
                return 0;
            }
            return rawTotalNanos / (double) pooledTotalNanos;
        }
    }

    BenchmarkResult run(int operations, int maxSize) {
        int boundedOps = Math.max(DashboardConfig.MIN_BENCHMARK_OPERATIONS,
            Math.min(operations, DashboardConfig.MAX_BENCHMARK_OPERATIONS));
        int boundedMax = Math.max(1, maxSize);

        warmup();
        long pooledNanos = runPooledBenchmark(boundedOps, boundedMax);
        long rawNanos = runRawBenchmark(boundedOps);
        return new BenchmarkResult(boundedOps, pooledNanos, rawNanos);
    }

    private static long runPooledBenchmark(int operations, int maxSize) {
        final int[] seed = {0};
        ObjectPool_main<BenchPayload> pool = new ObjectPool_main<>(
            new ObjectPoolOpts<>(
                Math.min(8, maxSize),
                maxSize,
                new CreateFunction<>() {
                    @Override
                    BenchPayload create() {
                        return new BenchPayload(++seed[0]);
                    }
                },
                new ResetFunction<>() {
                    @Override
                    BenchPayload reset(BenchPayload payload, Object... args) {
                        payload.counter = args.length > 0 && args[0] instanceof Integer ? (Integer) args[0] : payload.counter + 1;
                        payload.touch();
                        return payload;
                    }
                },
                -1
            )
        );

        long start = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            BenchPayload payload = pool.get(i);
            payload.touch();
            pool.free(payload);
        }
        long end = System.nanoTime();
        pool.dispose();
        return end - start;
    }

    private static long runRawBenchmark(int operations) {
        long start = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            BenchPayload payload = new BenchPayload(i);
            payload.touch();
        }
        long end = System.nanoTime();
        return end - start;
    }

    private static void warmup() {
        for (int i = 0; i < 5000; i++) {
            BenchPayload payload = new BenchPayload(i);
            payload.touch();
        }
    }

    private static final class BenchPayload {
        private int counter;
        private final byte[] block;

        private BenchPayload(int counter) {
            this.counter = counter;
            this.block = new byte[128];
        }

        private void touch() {
            int index = Math.abs(counter % block.length);
            block[index] = (byte) (counter & 0xFF);
            counter++;
        }
    }
}
