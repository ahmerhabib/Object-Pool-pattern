class ObjectPoolOpts<T> {
    int initialSize;
    int maxSize;
    CreateFunction<T> create;
    ResetFunction<T> reset;
    int collectFreq;

    ObjectPoolOpts(int initialSize, int maxSize, CreateFunction<T> create, ResetFunction<T> reset, int collectFreq) {
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.create = create;
        this.reset = reset;
        this.collectFreq = collectFreq;
    }
}
