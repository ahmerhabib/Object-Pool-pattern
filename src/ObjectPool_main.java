import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Scanner;

// ObjectPool class that manages object pooling
public class ObjectPool_main<T> {
    private static final int MAX_ALLOWED_POOL_SIZE = 10000;
    private static final int MIN_ALLOWED_COLLECT_FREQUENCY_MS = 100;
    private final List<T> activeObjects = new ArrayList<>();
    private final List<T> availableObjects = new ArrayList<>();
    private final ObjectPoolOpts<T> opts;
    private final Object lock = new Object();
    private Timer collectTimer;
    private boolean disposed;

    public ObjectPool_main(ObjectPoolOpts<T> opts) {
        this.opts = Objects.requireNonNull(opts, "opts cannot be null");
        validateOptions(opts);

        for (int i = 0; i < opts.initialSize; ++i) {
            availableObjects.add(opts.create.create());
        }

        if (opts.collectFreq != -1) {
            collectTimer = new Timer(true);
            collectTimer.schedule(new CollectTask(), opts.collectFreq, opts.collectFreq);
        }
    }

    public T get(Object... args) {
        synchronized (lock) {
            ensureNotDisposed();

            if (!availableObjects.isEmpty()) {
                T recycled = availableObjects.remove(availableObjects.size() - 1);
                T resetObj = opts.reset.reset(recycled, args);
                activeObjects.add(resetObj);
                return resetObj;
            }

            if (activeObjects.size() + availableObjects.size() >= opts.maxSize) {
                return null;
            }

            T obj = opts.create.create();
            activeObjects.add(obj);
            return obj;
        }
    }

    public void free(T obj) {
        synchronized (lock) {
            ensureNotDisposed();
            int index = activeObjects.indexOf(obj);
            if (index != -1) {
                activeObjects.remove(index);
                availableObjects.add(obj);
            }
        }
    }

    public void dispose() {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            disposed = true;

            if (collectTimer != null) {
                collectTimer.cancel();
                collectTimer = null;
            }

            activeObjects.clear();
            availableObjects.clear();
        }
    }

    private class CollectTask extends TimerTask {
        @Override
        public void run() {
            collectAvailableObjects();
        }
    }

    void collectAvailableObjects() {
        synchronized (lock) {
            if (disposed || availableObjects.size() <= opts.maxSize) {
                return;
            }
            availableObjects.subList(opts.maxSize, availableObjects.size()).clear();
        }
    }

    int activeCount() {
        synchronized (lock) {
            return activeObjects.size();
        }
    }

    int availableCount() {
        synchronized (lock) {
            return availableObjects.size();
        }
    }

    List<T> activeSnapshot() {
        synchronized (lock) {
            return new ArrayList<>(activeObjects);
        }
    }

    List<T> availableSnapshot() {
        synchronized (lock) {
            return new ArrayList<>(availableObjects);
        }
    }

    int maxSize() {
        return opts.maxSize;
    }

    private void ensureNotDisposed() {
        if (disposed) {
            throw new IllegalStateException("Object pool has been disposed.");
        }
    }

    private static <U> void validateOptions(ObjectPoolOpts<U> opts) {
        Objects.requireNonNull(opts.create, "create cannot be null");
        Objects.requireNonNull(opts.reset, "reset cannot be null");
        if (opts.maxSize < 0) {
            throw new IllegalArgumentException("maxSize must be >= 0");
        }
        if (opts.maxSize > MAX_ALLOWED_POOL_SIZE) {
            throw new IllegalArgumentException("maxSize must be <= " + MAX_ALLOWED_POOL_SIZE);
        }
        if (opts.initialSize < 0) {
            throw new IllegalArgumentException("initialSize must be >= 0");
        }
        if (opts.initialSize > opts.maxSize) {
            throw new IllegalArgumentException("initialSize must be <= maxSize");
        }
        if (opts.collectFreq != -1 && opts.collectFreq < MIN_ALLOWED_COLLECT_FREQUENCY_MS) {
            throw new IllegalArgumentException("collectFreq must be -1 or >= " + MIN_ALLOWED_COLLECT_FREQUENCY_MS + "ms");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter initial pool size: ");
        int initialSize = scanner.nextInt();

        System.out.print("Enter maximum pool size: ");
        int maxSize = scanner.nextInt();

        System.out.print("Enter collect frequency (-1 for no collection): ");
        int collectFreq = scanner.nextInt();
        final int[] sequence = {0};

        ObjectPoolOpts<Integer> opts = new ObjectPoolOpts<>(
            initialSize,
            maxSize,
            new CreateFunction<>() {
                @Override
                Integer create() {
                    return ++sequence[0];
                }
            },
            new ResetFunction<>() {
                @Override
                Integer reset(Integer obj, Object... args) {
                    if (args.length > 0 && args[0] instanceof Integer) {
                        return (Integer) args[0];
                    }
                    return obj;
                }
            },
            collectFreq
        );

        ObjectPool_main<Integer> pool = new ObjectPool_main<>(opts);

        // Testing the object pool
        Integer obj1 = pool.get();
        System.out.println("Got object: " + obj1);
        pool.free(obj1);

        // Dispose the pool
        pool.dispose();

        scanner.close();
    }
}
