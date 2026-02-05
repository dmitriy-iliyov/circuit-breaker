package io.github.dmitriyiliyov.circuitbreaker.core.strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleMovingAverageStrategy implements ObserveStrategy {

    private final int windowSize;
    private final Function<Exception, Boolean> checker;
    private final double threshold;
    private final Runnable callback;
    private int [] window;
    private int index;
    private int windowSum;
    private int actuallyWindowSize;
    private final Lock lock = new ReentrantLock();

    public SimpleMovingAverageStrategy(int windowSize, Function<Exception, Boolean> checker, double threshold,
                                       Runnable callback) {
        this.windowSize = windowSize;
        this.checker = checker;
        this.threshold = threshold;
        this.callback = callback;
        reset();
    }

    @Override
    public void observe(Runnable process) {
        try {
            process.run();
            updateWindow(0);
        } catch (Exception e) {
            handelException(e);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process) {
        try {
            T response = process.get();
            updateWindow(0);
            return response;
        } catch (Exception e) {
            handelException(e);
            throw e;
        }
    }

    private void handelException(Exception e) {
        if (checker.apply(e)) {
            updateWindow(1);
        }
    }

    private void updateWindow(int value) {
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
