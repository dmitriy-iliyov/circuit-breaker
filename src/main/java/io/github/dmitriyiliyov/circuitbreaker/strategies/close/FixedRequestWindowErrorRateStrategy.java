package io.github.dmitriyiliyov.circuitbreaker.strategies.close;

import java.util.function.Function;
import java.util.function.Supplier;

public class FixedRequestWindowErrorRateStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final double threshold;
    private int involveCount;
    private int observableExceptionCount;

    public FixedRequestWindowErrorRateStrategy(int windowSize, double threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
        reset();
    }

    @Override
    public void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback) {
        involveCount++;
        try {
            process.run();
        } catch (Exception e) {
            handelException(e, checker, callback);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback) {
        involveCount++;
        try {
            return process.get();
        } catch (Exception e) {
            handelException(e, checker, callback);
            throw e;
        }
    }

    public void handelException(Exception e, Function<Exception, Boolean> checker, Runnable callback) {
        if (!checker.apply(e)) {
            return;
        }
        if (involveCount >= windowSize) {
            reset();
            involveCount++;
            observableExceptionCount++;
            return;
        }
        observableExceptionCount++;
        double currentFrequency = (double) observableExceptionCount / involveCount;
        if (currentFrequency >= threshold) {
            callback.run();
        }
    }

    @Override
    public void reset() {
        involveCount = 0;
        observableExceptionCount = 0;
    }
}
