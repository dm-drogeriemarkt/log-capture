package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PACKAGE;

@RequiredArgsConstructor(access = PACKAGE)
public class LogAsserter {
    private final CapturingAppender capturingAppender;
    private final List<MatchingCondition> globalMatchingConditions;

    public NothingElseLoggedAsserter assertLoggedMessage(LogAssertion logAssertion, LogAssertion... moreLogAssertions) {
        LinkedList<LogAssertion> logAssertions = new LinkedList<>();
        logAssertions.add(logAssertion);
        logAssertions.addAll(Arrays.asList(moreLogAssertions));

        for (LogAssertion assertion : logAssertions) {
            assertLoggedMessage(assertion.level, assertion.regex, Optional.empty(), assertion.matchingConditions);
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
            lastCapturedLogEvent = Optional.of(assertLoggedMessage(assertion.level, assertion.regex, lastCapturedLogEvent, assertion.matchingConditions));
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
            nothingElseLogged = capturingAppender.getNumberOfLoggedMessages() == numberOfAssertedLogMessages;
        }

        public void assertNothingElseLogged() {
            if (!nothingElseLogged) {
                throw new AssertionError("There have been other log messages than the asserted ones.");
            }
        }
    }

    private LastCapturedLogEvent assertLoggedMessage(Level level, String regex, //TODO: format according to editorconfig
                                                     Optional<LastCapturedLogEvent> lastCapturedLogEvent,
                                                     List<MatchingCondition> localMatchingConditions) {
        if (capturingAppender == null) {
            throw new IllegalStateException("capuringAppender is null. " +
                    "Please make sure that either LogCapture is used with a @Rule annotation or that addAppenderAndSetLogLevelToTrace is called manually.");
        }

        Integer startIndex = lastCapturedLogEvent.isPresent() ? lastCapturedLogEvent.get().lastAssertedLogMessageIndex + 1 : 0;
        int numberOfAssertedLogMessages = lastCapturedLogEvent.isPresent() ? lastCapturedLogEvent.get().numberOfAssertedLogMessages + 1 : 1;

        LinkedList<MatchingCondition> matchingConditions = new LinkedList<>();
        matchingConditions.addAll(globalMatchingConditions);
        matchingConditions.addAll(localMatchingConditions);

        List<ExpectedMdcEntry> expectedMdcEntries = matchingConditions.stream().map(MatchingCondition::getExpectedMdcEntry).collect(Collectors.toList()); //TODO: move this logic to capturingAppender
        Integer foundAtIndex = capturingAppender.assertCapturedNext(level, regex, startIndex, expectedMdcEntries);

        return new LastCapturedLogEvent(foundAtIndex, numberOfAssertedLogMessages);
    }


}

