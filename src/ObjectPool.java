import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

interface CreateFunction_test<T> {
    T create();
}

interface ResetFunction_test<T> {
    T reset(T obj);
}

public class ObjectPool<T> {
    private List<T> activeObjects = new ArrayList<>();
    private List<T> availableObjects = new ArrayList<>();
    private CreateFunction_test<T> CreateFunction_test;
    private ResetFunction_test<T> ResetFunction_test;
    private int maxSize;

    public ObjectPool(CreateFunction_test<T> CreateFunction_test, ResetFunction_test<T> ResetFunction_test, int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be greater than 0");
        }
        this.CreateFunction_test = CreateFunction_test;
        this.ResetFunction_test = ResetFunction_test;
        this.maxSize = maxSize;
    }

    public T get() {
        if (availableObjects.isEmpty() && activeObjects.size() < maxSize) {
            T newObj = CreateFunction_test.create();
            activeObjects.add(newObj);
            return newObj;
        } else if (!availableObjects.isEmpty()) {
            T obj = availableObjects.remove(availableObjects.size() - 1);
            activeObjects.add(obj);
            return obj;
        } else {
            return null; // Pool is full
        }
    }

    public void free(T obj) {
        if (activeObjects.remove(obj)) {
            T resetObj = ResetFunction_test.reset(obj);
            availableObjects.add(resetObj);
        }
    }

    public void clear() {
        availableObjects.clear();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter maximum pool size: ");
        int maxSize = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        ObjectPool<Integer> pool = new ObjectPool<>(
            new CreateFunction_test<Integer>() {
                @Override
                public Integer create() {
                    return 0; // Create function
                }
            },
            new ResetFunction_test<Integer>() {
                @Override
                public Integer reset(Integer obj) {
                    return obj; // Reset function
                }
            },
            maxSize
        );

        while (true) {
            System.out.println("1. Get object from pool");
            System.out.println("2. Free object to pool");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    Integer obj = pool.get();
                    if (obj != null) {
                        System.out.println("Got object: " + obj);
                    } else {
                        System.out.println("Pool is full, cannot get object.");
                    }
                    break;
                case 2:
                    System.out.print("Enter object value to free: ");
                    int value = scanner.nextInt();
                    scanner.nextLine(); // Consume newline
                    pool.free(value);
                    System.out.println("Object freed.");
                    break;
                case 3:
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
