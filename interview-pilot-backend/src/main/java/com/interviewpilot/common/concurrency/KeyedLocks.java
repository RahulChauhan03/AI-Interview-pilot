package com.interviewpilot.common.concurrency;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Runs actions for the same key one at a time, so a double-click or a repeated request waits for the first
 * one and then finds (and reuses) the record it created instead of creating a second one. Locks are striped:
 * a fixed set shared by hash, so memory stays constant; unrelated keys rarely wait for each other.
 * Works within one application instance; callers still check for an existing record inside the lock.
 * Callers must not hold a database transaction while waiting, so the first request's data is committed.
 */
@Component
public class KeyedLocks {

    private static final int STRIPES = 64;
    private final ReentrantLock[] locks = new ReentrantLock[STRIPES];

    public KeyedLocks() {
        for (int i = 0; i < STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks[Math.floorMod(key.hashCode(), STRIPES)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
