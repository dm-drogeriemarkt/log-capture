package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static lombok.AccessLevel.PACKAGE;

/**
 * class doing the actual assertions of log messages
 */
@RequiredArgsConstructor(access = PACKAGE)
public class LogAsserter {
    private final CapturingAppender capturingAppender;
    private final List<LogEventMatcher> globalLogEventMatchers;

    /**
     * assert that multiple log messages have been logged in any order
     *
     * @param logExpectations descriptions of expected log messages
     *
     * @return asserter that can be used to check if anything else has been logged
     *
     * @throws AssertionError if any of the expected log message has not been logged or matching is imprecise (in case multiple expectations match the same message)
     * @throws IllegalArgumentException if less than two LogExpectations are provided
     */
    public NothingElseLoggedAsserter assertLoggedInAnyOrder(LogExpectation... logExpectations) {
        if (logExpectations.length < 2) {
            throw new IllegalArgumentException("at least 2 LogExpectations are required for assertLoggedInAnyOrder(). Found " +
                    (logExpectations.length == 1 ? logExpectations[0] : "none"));
        }

        Map<Integer, LogExpectation> matches = new HashMap<>();

        for (LogExpectation assertion : logExpectations) {
            LastCapturedLogEvent lastCapturedLogEvent = assertCapturedNext(assertion.level, assertion.regex, Optional.empty(), assertion.logEventMatchers);
            if (matches.containsKey(lastCapturedLogEvent.lastAssertedLogMessageIndex)) {
                LogExpectation previousMatch = matches.get(lastCapturedLogEvent.lastAssertedLogMessageIndex);
                throw new AssertionError(String.format(
                        "Imprecise matching: Two log expectations have matched the same message. " +
                                "Use more precise matching or in-order matching. " +
                                "(First match: %s | Second match: %s",
                        getDescriptionForUnwantedLogMessage(previousMatch.level, previousMatch.regex, previousMatch.logEventMatchers),
                        getDescriptionForUnwantedLogMessage(assertion.level, assertion.regex, assertion.logEventMatchers)));
            }
            matches.put(lastCapturedLogEvent.lastAssertedLogMessageIndex, assertion);
        }

        return new NothingElseLoggedAsserter(logExpectations.length);
    }

    /**
     * assert a single multiple message has been logged
     *
     * @param logExpectation descriptions of expected log message
     *
     * @return asserter that can be used to check if anything else has been logged
     *
     * @throws AssertionError if the expected log message has not been logged
     */
    public NothingElseLoggedAsserter assertLogged(LogExpectation logExpectation) {
        assertCapturedNext(logExpectation.level, logExpectation.regex, Optional.empty(), logExpectation.logEventMatchers);
        return new NothingElseLoggedAsserter(1);
    }

    /**
     * assert a message has been logged the given times
     *
     * @param logExpectation descriptions of expected log message
     *
     * @return asserter that can be used to check if anything else has been logged
     *
     * @throws AssertionError if the expected log message has not been logged as often as expected
     */
    public NothingElseLoggedAsserter assertLogged(LogExpectation logExpectation, Times times) {

        int matches = assertCapturedMultipleTimes(logExpectation.level, logExpectation.regex, logExpectation.logEventMatchers);

        boolean failAssertion = switch (times.getOperator()) {
            case EQUAL -> matches != times.getTimes();
            case AT_LEAST -> matches < times.getTimes();
            case AT_MOST -> matches > times.getTimes();
        };
        if (failAssertion) {
            throw new AssertionError("""
                    Log message has not occurred as often as expected:
                        Message: %s
                        Expected: %s
                        Actual: %s
                    """.formatted(logExpectation.regex.get(), times, matches));
        }
        return new NothingElseLoggedAsserter(1);
    }

    /**
     * assert that multiple log messages have been logged in an expected order
     *
     * @param logExpectations descriptions of expected log messages, in order
     *
     * @return asserter that can be used to check if anything else has been logged
     *
     * @throws AssertionError if any of the expected log message has not been logged or have been logged in the wrong order
     * @throws IllegalArgumentException if less than two LogExpectations are provided
     */
    public NothingElseLoggedAsserter assertLoggedInOrder(LogExpectation... logExpectations) {
        if (logExpectations.length < 2) {
            throw new IllegalArgumentException("at least 2 LogExpectations are required for assertLoggedInOrder(). Found " +
                    (logExpectations.length == 1 ? logExpectations[0] : "none"));
        }

        Optional<LastCapturedLogEvent> lastCapturedLogEvent = Optional.empty();
        for (LogExpectation assertion : logExpectations) {
            lastCapturedLogEvent = Optional.of(assertCapturedNext(assertion.level, assertion.regex, lastCapturedLogEvent, assertion.logEventMatchers));
        }

        return new NothingElseLoggedAsserter(logExpectations.length);
    }


    /**
     * assert that no matching log-message was logged
     *
     * @param logExpectations descriptions of log messages that should not occur
     *
     * @throws AssertionError if any of the expected log message has been logged
     * @throws IllegalArgumentException if no LogExpectation is provided
     */
    public void assertNotLogged(LogExpectation... logExpectations) {
        if (logExpectations.length < 1) {
            throw new IllegalArgumentException("at least one LogExpectation is required for assertNotLogged(). Found none");
        }


        for (LogExpectation assertion : logExpectations) {
            assertNotCaptured(assertion.level, assertion.regex, assertion.logEventMatchers);
        }
    }

    @RequiredArgsConstructor
    private static final class LastCapturedLogEvent {
        private final int lastAssertedLogMessageIndex;
        private final int numberOfAssertedLogMessages;
    }

    /**
     * asserter to check nothing else has been logged
     */
    public final class NothingElseLoggedAsserter {
        private final boolean nothingElseLogged;

        private NothingElseLoggedAsserter(int numberOfAssertedLogMessages) {
            nothingElseLogged = capturingAppender.loggedEvents.size() == numberOfAssertedLogMessages;
        }

        /**
         * assert that nothing else has been logged (as far as log messages have been captured) see @{@link LogCapture} on how to configure this
         *
         * @throws AssertionError if anything unexpected has been logged
         */
        public void assertNothingElseLogged() {
            if (!nothingElseLogged) {
                throw new AssertionError("There have been other log messages than the asserted ones.");
            }
        }
    }

    private LastCapturedLogEvent assertCapturedNext(Optional<Level> level, Optional<String> regex,
                                                    Optional<LastCapturedLogEvent> optionalLastCapturedLogEvent,
                                                    List<LogEventMatcher> localLogEventMatchers) {
        if (capturingAppender == null) {
            throw new IllegalStateException("capturingAppender is null. " +
                    "Please make sure that either LogCapture is used with a @Rule annotation or that addAppenderAndSetLogLevelToTrace is called manually.");
        }

        int startIndex = optionalLastCapturedLogEvent.map(capturedLogEvent -> capturedLogEvent.lastAssertedLogMessageIndex + 1).orElse(0);
        int numberOfAssertedLogMessages = optionalLastCapturedLogEvent.map(capturedLogEvent -> capturedLogEvent.numberOfAssertedLogMessages + 1).orElse(1);

        LinkedList<LogEventMatcher> logEventMatchers = new LinkedList<>();
        logEventMatchers.addAll(globalLogEventMatchers);
        logEventMatchers.addAll(localLogEventMatchers);

        Integer foundAtIndex = assertCapturedNext(level, regex, startIndex, logEventMatchers);

        return new LastCapturedLogEvent(foundAtIndex, numberOfAssertedLogMessages);
    }

    Integer assertCapturedNext(Optional<Level> level, Optional<String> regex, int startIndex, List<LogEventMatcher> logEventMatchers) {
        Pattern pattern = Pattern.compile(".*" + regex.orElse("") + ".*", Pattern.DOTALL + Pattern.MULTILINE);
        LoggedEvent eventMatchingWithoutAdditionalMatchers = null;
        for (int i = startIndex; i < capturingAppender.loggedEvents.size(); i++) {
            LoggedEvent event = capturingAppender.loggedEvents.get(i);
            if (eventMatchesWithoutAdditionalMatchers(event, level, pattern)) {
                if (isMatchedByAll(event, logEventMatchers)) {
                    return i;
                }
                eventMatchingWithoutAdditionalMatchers = event;
            }
        }
        if (eventMatchingWithoutAdditionalMatchers != null) {
            throwAssertionForPartiallyMatchingLoggedEvent(level, regex, eventMatchingWithoutAdditionalMatchers, logEventMatchers);
        }
        throw new AssertionError(format("Expected log message has not occurred: %s", getDescriptionForExpectedMessage(level, regex)));
    }

    void assertNotCaptured(Optional<Level> level, Optional<String> regex, List<LogEventMatcher> logEventMatchers) {
        Pattern pattern = Pattern.compile(".*" + regex.orElse("") + ".*", Pattern.DOTALL + Pattern.MULTILINE);

        for (int i = 0; i < capturingAppender.loggedEvents.size(); i++) {
            LoggedEvent event = capturingAppender.loggedEvents.get(i);
            if (eventMatchesWithoutAdditionalMatchers(event, level, pattern) && isMatchedByAll(event, logEventMatchers)) {
                throw new AssertionError(format("Found a log message that should not be logged: %s", getDescriptionForUnwantedLogMessage(level, regex, logEventMatchers)));
            }
        }
    }

    private int assertCapturedMultipleTimes(Optional<Level> level, Optional<String> regex, List<LogEventMatcher> logEventMatchers) {
        Pattern pattern = Pattern.compile(".*" + regex.orElse("") + ".*", Pattern.DOTALL + Pattern.MULTILINE);

        int matches = 0;
        for (int i = 0; i < capturingAppender.loggedEvents.size(); i++) {
            LoggedEvent event = capturingAppender.loggedEvents.get(i);
            if (eventMatchesWithoutAdditionalMatchers(event, level, pattern) && isMatchedByAll(event, logEventMatchers)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean eventMatchesWithoutAdditionalMatchers(LoggedEvent event, Optional<Level> level, Pattern pattern) {
        return eventMatchesLevel(event, level) && eventMatchesPattern(event, pattern);
    }

    private static void throwAssertionForPartiallyMatchingLoggedEvent(Optional<Level> level, Optional<String> regex, LoggedEvent partiallyMatchingLoggedEvent, List<LogEventMatcher> logEventMatchers) {
        StringBuilder assertionMessage = new StringBuilder();

        for (LogEventMatcher logEventMatcher : logEventMatchers) {
            if (!logEventMatcher.matches(partiallyMatchingLoggedEvent)) {
                assertionMessage.append(format("Expected log message has occurred, but never with the expected %s: %s",
                        logEventMatcher.getMatcherTypeDescription(), getDescriptionForExpectedMessage(level, regex)));
                assertionMessage.append(lineSeparator());
                assertionMessage.append(logEventMatcher.getNonMatchingErrorMessage(partiallyMatchingLoggedEvent));
                assertionMessage.append(lineSeparator());
            }
        }
        throw new AssertionError(assertionMessage.toString());
    }

    private static boolean eventMatchesPattern(LoggedEvent event, Pattern pattern) {
        return pattern.matcher(event.getFormattedMessage()).matches();
    }

    private static boolean eventMatchesLevel(LoggedEvent event, Optional<Level> expectedLevel) {
        return expectedLevel
                .map(expected -> event.getLevel().equals(expected))
                .orElse(true);
    }

    static boolean isMatchedByAll(LoggedEvent loggedEvent, List<? extends LogEventMatcher> logEventMatchers) {
        if (logEventMatchers == null) {
            return true;
        }
        return logEventMatchers.stream().allMatch(matcher -> matcher.matches(loggedEvent));
    }

    @SuppressWarnings("squid:S1192") // a constant for "Level: " is not helpful
    private static String getDescriptionForExpectedMessage(Optional<Level> level, Optional<String> regex) {

        if (level.isEmpty() && regex.isEmpty()) {
            return "<Any log message>";
        }
        if (level.isPresent() && regex.isPresent()) {
            return "Level: " + level.get() + ", Regex: \"" + regex.get() + "\"";
        }
        return (level.map(value -> "Level: " + value).orElse("")) +
                (regex.map(s -> "Regex: \"" + s + "\"").orElse(""));
    }

    private static String getDescriptionForUnwantedLogMessage(Optional<Level> level, Optional<String> regex, List<LogEventMatcher> matchers) {
        String matchersText = "";
        if (matchers != null && !matchers.isEmpty()) {
            matchersText = ", with matchers:" + lineSeparator() + "  " + matchers.stream().map(LogEventMatcher::getMatcherDetailDescription)
                    .collect(Collectors.joining(lineSeparator() + "  "));
        }
        if (level.isEmpty() && regex.isEmpty()) {
            return "<Any log message>" + matchersText;
        }
        if (level.isPresent() && regex.isPresent()) {
            return "Level: " + level.get() + ", Regex: \"" + regex.get() + "\"" + matchersText;
        }
        return (level.map(value -> "Level: " + value).orElse("")) +
                (regex.map(s -> "Regex: \"" + s + "\"").orElse("")) + matchersText;
    }
}

