class ResetFunction<T> {
    T reset(T obj, Object... args) {
        // Override in pool configuration to reset/reinitialize recycled instances.
        return obj;
    }
}
