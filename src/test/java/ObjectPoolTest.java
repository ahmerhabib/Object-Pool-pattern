import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ObjectPoolTest {
    @Test
    void freeStoresResetReturnValue() {
        ObjectPool<Integer> pool = new ObjectPool<>(
            () -> 10,
            obj -> obj + 1,
            1
        );

        Integer created = pool.get();
        pool.free(created);
        Integer recycled = pool.get();

        assertEquals(11, recycled);
    }

    @Test
    void getReturnsNullWhenPoolIsAtCapacity() {
        ObjectPool<Integer> pool = new ObjectPool<>(
            () -> 1,
            obj -> obj,
            1
        );

        Integer first = pool.get();
        Integer second = pool.get();

        assertEquals(1, first);
        assertNull(second);
    }
}
