package de.dm.infrastructure.logcapture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static java.lang.String.format;

/**
 * define expected StructuredArgument.keyValue(...) from logstash with this matcher
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExpectedKeyValue implements LogEventMatcher {
    static final String LOGSTASH_MARKER_CLASS = "net.logstash.logback.marker.SingleFieldAppendingMarker";
    private final String expectedKey;
    private final String expectedValue;

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        failIfLogstashIsNotInClasspath();
        return ExpectedKeyValueLogstashDelegate.matches(loggedEvent, expectedKey, expectedValue);
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        failIfLogstashIsNotInClasspath();
        return ExpectedKeyValueLogstashDelegate.getNonMatchingErrorMessage(loggedEvent, expectedKey, expectedValue);
    }

    @Override
    public String getMatcherTypeDescription() {
        return "key-value content";
    }

    @Override
    public String getMatcherDetailDescription() {
        return format("keyValue content with key: \"%s\" and value: \"%s\"", expectedKey, expectedValue);
    }

    /**
     * use this in a log expectation to verify that something has been logged with a keyValue argument from logstashg's StructuredArgument
     *
     * @param expectedKey expected key
     * @param expectedValue expected value
     *
     * @return expected keyValue to use in log expectation
     */
    public static ExpectedKeyValue keyValue(String expectedKey, String expectedValue) {
        return new ExpectedKeyValue(expectedKey, expectedValue);
    }

    /**
     * use this in a log expectation to verify that something has been logged with a keyValue argument from logstashg's StructuredArgument
     *
     * @param expectedKey expected key
     * @param expectedValue expected value
     *
     * @return expected keyValue to use in log expectation
     */
    public static ExpectedKeyValue keyValue(String expectedKey, int expectedValue) {
        return new ExpectedKeyValue(expectedKey, String.valueOf(expectedValue));
    }

    /**
     * use this in a log expectation to verify that something has been logged with a keyValue argument from logstashg's StructuredArgument
     *
     * @param expectedKey expected key
     * @param expectedValue expected value
     *
     * @return expected keyValue to use in log expectation
     */
    public static ExpectedKeyValue keyValue(String expectedKey, long expectedValue) {
        return new ExpectedKeyValue(expectedKey, String.valueOf(expectedValue));
    }

    private void failIfLogstashIsNotInClasspath() {
        try {
            Class.forName(LOGSTASH_MARKER_CLASS, false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("keyValue cannot be used for log assertions if " +
                    "logstash-logback-encoder that provides StructuredArguments.keyValue(...) is not in the classpath.");
        }
    }
}
