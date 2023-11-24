package de.dm.infrastructure.logcapture;

import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

/**
 * define expected key-value pair to be attached to a log message
 */
public final class ExpectedKeyValue implements LogEventMatcher {
    private final String key;
    private final Object value;

    private ExpectedKeyValue(String key, Object value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key and value are required for key-value log assertion");
        }
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        return loggedEvent.getKeyValuePairs() != null &&
                loggedEvent.getKeyValuePairs().stream()
                        .anyMatch(pair -> key.equals(pair.key) && (
                                Objects.equals(value, pair.value) ||
                                        areEqualAsNumbers(value, pair.value)
                        ));
    }

    /*
     * this is only done for Numbers because
     * 1. toString() should not be expensive for these
     * 2. when Logging 2L can be considered equal to 2, for example, but maybe not to 2.0
     */
    private boolean areEqualAsNumbers(Object expectedValue, Object actualValue) {
        return expectedValue instanceof Number && actualValue instanceof Number &&
                expectedValue.toString().equals(actualValue.toString());
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        String expected = format("  expected key-value pair (%s, %s)", key, value) + lineSeparator();
        return expected + format("  actual pairs: [%s]", loggedEvent.getKeyValuePairs() == null ? "" :
                loggedEvent.getKeyValuePairs().stream()
                        .map(pair -> "(%s, %s)".formatted(pair.key, pair.value))
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public String getMatcherTypeDescription() {
        return "key-value pair";
    }

    @Override
    public String getMatcherDetailDescription() {
        return format("key-value pair (%s, %s)", key, value);
    }

    /**
     * use this in a log expectation to verify that something has been logged with a certain key-value pair
     *
     * @param key expected key
     * @param value expected value
     *
     * @return expected key-value to use in log expectation
     */
    public static ExpectedKeyValue keyValue(String key, Object value) {
        return new ExpectedKeyValue(key, value);
    }
}
