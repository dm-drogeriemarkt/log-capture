package de.dm.infrastructure.logcapture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.SingleFieldAppendingMarker;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExpectedKeyValue implements LogEventMatcher {
    private final String expectedKey;
    private final Object expectedValue;

    // TODO: extract everything referencing net.logstash into a delegate that is only called if that is present in the classpath
    // otherwise, fail with an error message describing that keyValue can only be asserted with logstash in the classpath

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        return Arrays.stream(loggedEvent.getArgumentArray())
                .flatMap(argument -> argument instanceof SingleFieldAppendingMarker ? Stream.of((SingleFieldAppendingMarker) argument) : Stream.empty())
                .anyMatch(marker -> marker.getFieldValue().equals(expectedValue) && marker.getFieldName().equals(expectedKey));
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        String actualKeyValue = Arrays.stream(loggedEvent.getArgumentArray())
                .flatMap(argument -> argument instanceof SingleFieldAppendingMarker ? Stream.of((SingleFieldAppendingMarker) argument) : Stream.empty())
                .map(marker -> format("    key: \"%s\", value: \"%s\"", marker.getFieldName(), marker.getFieldValue()))
                .collect(Collectors.joining(lineSeparator()));

        return format("  expected key-value content: key: \"%s\", value: \"%s\"", expectedKey, expectedValue) +
                lineSeparator() +
                (actualKeyValue.isEmpty() ? "  but no key-value content was found" : "  actual key-value content:" + lineSeparator() + actualKeyValue);

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
}
