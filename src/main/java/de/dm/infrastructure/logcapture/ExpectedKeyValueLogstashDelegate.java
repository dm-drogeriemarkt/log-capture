package de.dm.infrastructure.logcapture;

import net.logstash.logback.marker.SingleFieldAppendingMarker;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

// this is a delegate accessing net.logstash so that ExpectedKeyValue can fail gracefully
// if logstash is not in the classpath
final class ExpectedKeyValueLogstashDelegate {
    private ExpectedKeyValueLogstashDelegate() {
    }

    static boolean matches(LoggedEvent loggedEvent, String expectedKey, String expectedValue) {
        return Arrays.stream(loggedEvent.getArgumentArray())
                .flatMap(argument -> argument instanceof SingleFieldAppendingMarker ? Stream.of((SingleFieldAppendingMarker) argument) : Stream.empty())
                .anyMatch(marker -> expectedValue.equals(marker.getFieldValue()) && marker.getFieldName().equals(expectedKey));
    }

    static String getNonMatchingErrorMessage(LoggedEvent loggedEvent, String expectedKey, String expectedValue) {
        String actualKeyValue = Arrays.stream(loggedEvent.getArgumentArray())
                .flatMap(argument -> argument instanceof SingleFieldAppendingMarker ? Stream.of((SingleFieldAppendingMarker) argument) : Stream.empty())
                .map(marker -> format("    key: \"%s\", value: \"%s\"", marker.getFieldName(), marker.getFieldValue()))
                .collect(Collectors.joining(lineSeparator()));

        return format("  expected key-value content: key: \"%s\", value: \"%s\"", expectedKey, expectedValue) +
                lineSeparator() +
                (actualKeyValue.isEmpty() ? "  but no key-value content was found" : "  actual key-value content:" + lineSeparator() + actualKeyValue);

    }

}
