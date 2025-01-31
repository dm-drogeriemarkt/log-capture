package de.dm.infrastructure.logcapture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static de.dm.infrastructure.logcapture.ExpectedTimes.ComparisonStrategy.AT_LEAST;
import static de.dm.infrastructure.logcapture.ExpectedTimes.ComparisonStrategy.AT_MOST;
import static de.dm.infrastructure.logcapture.ExpectedTimes.ComparisonStrategy.EQUAL;
import static lombok.AccessLevel.PRIVATE;

/**
 * define expected repetitions of log messages
 */
@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class ExpectedTimes {

    private final int referenceValue;
    private final ComparisonStrategy comparisonStrategy;

    @RequiredArgsConstructor
    enum ComparisonStrategy {
        EQUAL("exactly"),
        AT_LEAST("at least"),
        AT_MOST("at most");

        final String strategyName;
    }

    /**
     * Asserts that the log entry is logged exactly as often as the given times.
     *
     * <p>
     * <b>Note:</b> {@link LogCapture#assertNotLogged(LogExpectation...)} can be used
     * instead of {@code times(0)} to test that something has not been logged at all
     * </p>
     *
     * @param times exact number of expected log messages
     *
     * @return Times object which asserts with exactly the given times
     */
    public static ExpectedTimes times(int times) {

        if (times < 0) {
            throw new IllegalArgumentException("Number of log message occurrences that are expected must be positive.");
        }
        return new ExpectedTimes(times, EQUAL);
    }

    /**
     * Asserts that the log entry is logged exactly once.
     *
     * @return Times object which asserts with exactly once
     */
    public static ExpectedTimes once() {
        return new ExpectedTimes(1, EQUAL);
    }

    /**
     * Asserts that the log entry is logged at least as often as the given times.
     *
     * @param min minimum number of log message occurrences, must be at least 1
     *
     * @return Times object which asserts at least the given times
     */
    public static ExpectedTimes atLeast(int min) {
        if (min < 1) {
            throw new IllegalArgumentException("Minimum number of log message occurrences that are expected must be greater than 0.");
        }
        return new ExpectedTimes(min, AT_LEAST);
    }

    /**
     * Asserts that the log entry is logged at most as often as the given maximum.
     *
     * <p>
     * <b>Note:</b> {@link LogCapture#assertNotLogged(LogExpectation...)} can be used
     * instead of <code>atMost(0)</code> to test that something has not been logged at all
     * </p>
     *
     * @param max maximum number of log message occurrences, must be positive
     *
     * @return Times object which asserts at most the given times
     */
    public static ExpectedTimes atMost(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Maximum number of log message occurrences that are expected must be greater than 0.");
        }
        return new ExpectedTimes(max, AT_MOST);
    }
}
