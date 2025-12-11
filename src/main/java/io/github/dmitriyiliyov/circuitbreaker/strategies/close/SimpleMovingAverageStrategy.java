package io.github.dmitriyiliyov.circuitbreaker.strategies.close;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleMovingAverageStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final double threshold;
    private int [] window;
    private int index;
    private int windowSum;
    private int actuallyWindowSize;
    private final Lock lock = new ReentrantLock();

    public SimpleMovingAverageStrategy(int windowSize, double threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
        reset();
    }

    @Override
    public void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback) {
        try {
            process.run();
            updateWindow(0, callback);
        } catch (Exception e) {
            handelException(e, checker, callback);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback) {
        try {
            T response = process.get();
            updateWindow(0, callback);
            return response;
        } catch (Exception e) {
            handelException(e, checker, callback);
            throw e;
        }
    }

    private void handelException(Exception e, Function<Exception, Boolean> checker, Runnable callback) {
        if (checker.apply(e)) {
            updateWindow(1, callback);
        }
    }

    private void updateWindow(int value, Runnable callback) {
        lock.lock();
        try {
            if (actuallyWindowSize >= windowSize) {
                windowSum -= window[index];
            } else {
                actuallyWindowSize++;
            }
            windowSum += value;
            window[index] = value;
            index = (index + 1) % windowSize;
            if (actuallyWindowSize == windowSize && (double) windowSum / actuallyWindowSize >= threshold) {
                callback.run();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        window = new int[windowSize];
        index = 0;
        windowSum = 0;
        actuallyWindowSize = 0;
    }
}
