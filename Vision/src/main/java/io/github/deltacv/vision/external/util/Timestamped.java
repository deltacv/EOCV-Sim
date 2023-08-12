package io.github.deltacv.vision.external.util;

public class Timestamped<T> {

    private final T value;
    private final long timestamp;

    public Timestamped(T value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public T getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
