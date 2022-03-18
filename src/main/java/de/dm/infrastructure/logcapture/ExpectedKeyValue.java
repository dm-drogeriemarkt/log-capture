package de.dm.infrastructure.logcapture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExpectedKeyValue implements LogEventMatcher {
    static final String LOGSTASH_MARKER_CLASS = "net.logstash.logback.marker.SingleFieldAppendingMarker";
    private final String expectedKey;
    private final Object expectedValue;

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
    public String getMatcherDescription() {
        return "key-value content";
    }

    @Override
    public String getMatchingErrorMessage() {
        return null;
    }

    public static ExpectedKeyValue keyValue(String expectedKey, Object expectedValue) {
        return new ExpectedKeyValue(expectedKey, expectedValue);
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
