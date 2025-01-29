package de.dm.infrastructure.logcapture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static de.dm.infrastructure.logcapture.Times.Operator.AT_LEAST;
import static de.dm.infrastructure.logcapture.Times.Operator.AT_MOST;
import static de.dm.infrastructure.logcapture.Times.Operator.EQUAL;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class Times {

    private final int times;
    private final Operator operator;

    public enum Operator {
        /**
         * check for equal number of occurrences
         */
        EQUAL,
        /**
         * check for minimal number of occurrences
         */
        AT_LEAST,
        /**
         * check for maximum number of occurrences
         */
        AT_MOST
    }

    /**
     * Asserts that the log entry is logged exactly the given times.
     *
     * @param times number of times to be asserted
     *
     * @return Times object which asserts with exactly the given times
     *
     * @throws IllegalArgumentException if times is less than 2 (use atLeast, atMost or remove times as argument)
     */
    public static Times times(int times) {

        if (times < 2) {
            throw new IllegalArgumentException("Times must be greater than 1. If you want to test a single log entry, remove times as argument.");
        }
        return new Times(times, EQUAL);
    }

    /**
     * Asserts that the log entry is logged at least as often as the given times.
     *
     * @param times number of times to be asserted at least
     *
     * @return Times object which asserts at least the given times
     */
    public static Times atLeast(int times) {

        return new Times(times, AT_LEAST);
    }

    /**
     * Asserts that the log entry is logged at most as often as the given times.
     *
     * @param times number of times to be asserted at most
     *
     * @return Times object which asserts at most the given times
     */
    public static Times atMost(int times) {

        return new Times(times, AT_MOST);
    }
}
