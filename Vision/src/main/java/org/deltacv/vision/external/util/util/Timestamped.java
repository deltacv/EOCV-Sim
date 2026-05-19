/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.vision.external.util;

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

