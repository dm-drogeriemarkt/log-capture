package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static lombok.AccessLevel.PACKAGE;

@RequiredArgsConstructor(access = PACKAGE)
public class LogAsserter {
    private final CapturingAppender capturingAppender;
    private final List<LogEventMatcher> globalLogEventMatchers;

    public NothingElseLoggedAsserter assertLoggedInAnyOrder(LogAssertion logAssertion, LogAssertion... moreLogAssertions) {
        LinkedList<LogAssertion> logAssertions = new LinkedList<>();
        logAssertions.add(logAssertion);
        logAssertions.addAll(Arrays.asList(moreLogAssertions));

        Map<Integer, LogAssertion> matches = new HashMap<>();

        for (LogAssertion assertion : logAssertions) {
            LastCapturedLogEvent lastCapturedLogEvent = assertCapturedNext(assertion.level, assertion.regex, Optional.empty(), assertion.logEventMatchers);
            if (matches.containsKey(lastCapturedLogEvent.lastAssertedLogMessageIndex)) {
                LogAssertion previousMatch = matches.get(lastCapturedLogEvent.lastAssertedLogMessageIndex);
                throw new AssertionError(String.format(
                        "Imprecise matching: Two log assertions have matched the same message. " +
                                "Use more precise matching or in-order matching. " +
                                "(First match: Level: %s, Regex: \"%s\" | Second match: Level: %s, Regex: \"%s\"",
                        previousMatch.level, previousMatch.regex, assertion.level, assertion.regex));
            }
            matches.put(lastCapturedLogEvent.lastAssertedLogMessageIndex, assertion);
        }

        return new NothingElseLoggedAsserter(logAssertions.size());
    }

    public NothingElseLoggedAsserter assertLoggedInOrder(LogAssertion logAssertion, LogAssertion nextLogAssertion, LogAssertion... moreLogAssertions) {
        LinkedList<LogAssertion> logAssertions = new LinkedList<>();
        logAssertions.add(logAssertion);
        logAssertions.add(nextLogAssertion);
        logAssertions.addAll(Arrays.asList(moreLogAssertions));

        Optional<LastCapturedLogEvent> lastCapturedLogEvent = Optional.empty();
        for (LogAssertion assertion : logAssertions) {
            lastCapturedLogEvent = Optional.of(assertCapturedNext(assertion.level, assertion.regex, lastCapturedLogEvent, assertion.logEventMatchers));
        }

        return new NothingElseLoggedAsserter(logAssertions.size());
    }

    @RequiredArgsConstructor
    private static final class LastCapturedLogEvent {
        private final int lastAssertedLogMessageIndex;
        private final int numberOfAssertedLogMessages;
    }

    public final class NothingElseLoggedAsserter {
        private final boolean nothingElseLogged;

        private NothingElseLoggedAsserter(int numberOfAssertedLogMessages) {
            nothingElseLogged = capturingAppender.loggedEvents.size() == numberOfAssertedLogMessages;
        }

        public void assertNothingElseLogged() {
            if (!nothingElseLogged) {
                throw new AssertionError("There have been other log messages than the asserted ones.");
            }
        }
    }

    private LastCapturedLogEvent assertCapturedNext(Level level, String regex,
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

    Integer assertCapturedNext(Level level, String regex, int startIndex, List<LogEventMatcher> logEventMatchers) {
        Pattern pattern = Pattern.compile(".*" + regex + ".*", Pattern.DOTALL + Pattern.MULTILINE);
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
        throw new AssertionError(format("Expected log message has not occurred: Level: %s, Regex: \"%s\"", level, regex));
    }

    private boolean eventMatchesWithoutAdditionalMatchers(LoggedEvent event, Level level, Pattern pattern) {
        return eventHasLevel(event, level) && eventMatchesPattern(event, pattern);
    }

    private static void throwAssertionForPartiallyMatchingLoggedEvent(Level level, String regex, LoggedEvent partiallyMatchingLoggedEvent, List<LogEventMatcher> logEventMatchers) {
        StringBuilder assertionMessage = new StringBuilder();

        for (LogEventMatcher logEventMatcher : logEventMatchers) {
            assertionMessage.append(format("Expected log message has occurred, but never with the expected %s: Level: %s, Regex: \"%s\"",
                    logEventMatcher.getMatcherDescription(), level, regex));
            assertionMessage.append(lineSeparator());
            assertionMessage.append(logEventMatcher.getNonMatchingErrorMessage(partiallyMatchingLoggedEvent));
            assertionMessage.append(lineSeparator());
        }
        throw new AssertionError(assertionMessage.toString());
    }

    private static boolean eventMatchesPattern(LoggedEvent event, Pattern pattern) {
        return pattern.matcher(event.getFormattedMessage()).matches();
    }

    private static boolean eventHasLevel(LoggedEvent event, Level level) {
        return event.getLevel().equals(level);
    }

    static boolean isMatchedByAll(LoggedEvent loggedEvent, List<? extends LogEventMatcher> logEventMatchers) {
        if (logEventMatchers == null) {
            return true;
        }
        return logEventMatchers.stream().allMatch(matcher -> matcher.matches(loggedEvent));
    }
}

