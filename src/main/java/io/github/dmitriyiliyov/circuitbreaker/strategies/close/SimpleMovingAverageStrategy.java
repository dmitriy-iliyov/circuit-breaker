package io.github.dmitriyiliyov.circuitbreaker.strategies.close;

import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleMovingAverageStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final double threshold;
    private int [] window;
    private int index;
    private int windowSum;
    private int actualLyWindowSize;

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
        if (actualLyWindowSize >= windowSize) {
            windowSum -= window[index];
        } else {
            actualLyWindowSize++;
        }
        windowSum += value;
        window[index] = value;
        index = (index + 1) % windowSize;
        if (actualLyWindowSize == windowSize && (double) windowSum / actualLyWindowSize >= threshold) {
            callback.run();
        }
    }

    @Override
    public void reset() {
        window = new int[windowSize];
        index = 0;
        windowSum = 0;
        actualLyWindowSize = 0;
    }
}
