import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ObjectPoolMainTest {
    private static final class Box {
        private final int value;

        private Box(int value) {
            this.value = value;
        }
    }

    @Test
    void getRespectsMaxSize() {
        AtomicInteger sequence = new AtomicInteger(0);
        ObjectPool_main<Box> pool = new ObjectPool_main<>(
            new ObjectPoolOpts<>(
                0,
                1,
                new CreateFunction<>() {
                    @Override
                    Box create() {
                        return new Box(sequence.incrementAndGet());
                    }
                },
                new ResetFunction<>() {
                    @Override
                    Box reset(Box obj, Object... args) {
                        return obj;
                    }
                },
                -1
            )
        );

        Box first = pool.get();
        Box second = pool.get();

        assertEquals(1, first.value);
        assertNull(second);
        assertEquals(1, pool.activeCount());
        pool.dispose();
    }

    @Test
    void getPassesArgsToResetAndUsesResetReturnValue() {
        ObjectPool_main<Box> pool = new ObjectPool_main<>(
            new ObjectPoolOpts<>(
                1,
                1,
                new CreateFunction<>() {
                    @Override
                    Box create() {
                        return new Box(100);
                    }
                },
                new ResetFunction<>() {
                    @Override
                    Box reset(Box obj, Object... args) {
                        return new Box((Integer) args[0]);
                    }
                },
                -1
            )
        );

        Box recycled = pool.get(42);

        assertEquals(42, recycled.value);
        assertEquals(1, pool.activeCount());
        assertEquals(0, pool.availableCount());
        pool.dispose();
    }

    @Test
    void collectAvailableObjectsDoesNotThrowWhenBelowMax() {
        ObjectPool_main<Box> pool = new ObjectPool_main<>(
            new ObjectPoolOpts<>(
                1,
                3,
                new CreateFunction<>() {
                    @Override
                    Box create() {
                        return new Box(1);
                    }
                },
                new ResetFunction<>() {
                    @Override
                    Box reset(Box obj, Object... args) {
                        return obj;
                    }
                },
                -1
            )
        );

        assertDoesNotThrow(pool::collectAvailableObjects);
        assertEquals(1, pool.availableCount());
        pool.dispose();
    }

    @Test
    void disposePreventsFurtherUseWithoutNpe() {
        ObjectPool_main<Box> pool = new ObjectPool_main<>(
            new ObjectPoolOpts<>(
                0,
                1,
                new CreateFunction<>() {
                    @Override
                    Box create() {
                        return new Box(1);
                    }
                },
                new ResetFunction<>() {
                    @Override
                    Box reset(Box obj, Object... args) {
                        return obj;
                    }
                },
                -1
            )
        );

        pool.dispose();

        assertThrows(IllegalStateException.class, pool::get);
        assertThrows(IllegalStateException.class, () -> pool.free(new Box(1)));
    }

    @Test
    void concurrentGetAndFreeWithCollectorDoesNotThrow() throws Exception {
        AtomicInteger sequence = new AtomicInteger(0);
        ObjectPool_main<Box> pool = new ObjectPool_main<>(
            new ObjectPoolOpts<>(
                10,
                20,
                new CreateFunction<>() {
                    @Override
                    Box create() {
                        return new Box(sequence.incrementAndGet());
                    }
                },
                new ResetFunction<>() {
                    @Override
                    Box reset(Box obj, Object... args) {
                        return obj;
                    }
                },
                100
            )
        );

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 2000; j++) {
                        Box obj = pool.get(j);
                        if (obj != null) {
                            pool.free(obj);
                        }
                    }
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        pool.dispose();
        assertTrue(failures.isEmpty(), "Concurrent operations should not throw");
    }
}
