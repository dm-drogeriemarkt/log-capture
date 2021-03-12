package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import de.dm.infrastructure.logcapture.LogCapture.LastCapturedLogEvent;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PACKAGE)
public class FluentLogAssertion {
    private final LogCapture logCapture;
    private final Optional<LastCapturedLogEvent> lastCapturedLogEvent;
    private final List<ExpectedMdcEntry> expectedMdcEntries = new LinkedList<>();

    public FluentLogAssertion withMdcForAll(String key, String regex) {
        FluentLogAssertion newAssertion = new FluentLogAssertion(logCapture, lastCapturedLogEvent);
        newAssertion.expectedMdcEntries.addAll(expectedMdcEntries);
        newAssertion.expectedMdcEntries.add(ExpectedMdcEntry.withMdc(key, regex));

        return newAssertion;
    }

    public ConfiguredLogAssertion warn() {
        return new ConfiguredLogAssertion(WARN);
    }

    public ConfiguredLogAssertion error() {
        return new ConfiguredLogAssertion(ERROR);
    }

    public ConfiguredLogAssertion info() {
        return new ConfiguredLogAssertion(INFO);
    }

    public ConfiguredLogAssertion debug() {
        return new ConfiguredLogAssertion(DEBUG);
    }

    public ConfiguredLogAssertion trace() {
        return new ConfiguredLogAssertion(TRACE);
    }

    @RequiredArgsConstructor(access = PRIVATE)
    public class ConfiguredLogAssertion {
        private final Level level;
        private final List<ExpectedMdcEntry> expectedMdcEntries = new LinkedList<>(FluentLogAssertion.this.expectedMdcEntries);

        public ConfiguredLogAssertion withMdc(String key, String regex) {
            ConfiguredLogAssertion newAssertion = new ConfiguredLogAssertion(level);
            newAssertion.expectedMdcEntries.addAll(expectedMdcEntries);
            newAssertion.expectedMdcEntries.add(ExpectedMdcEntry.withMdc(key, regex));

            return newAssertion;
        }

        public FluentLogAssertion assertMessage(String regex) {
            final LastCapturedLogEvent capturedLogEvent;
            if (lastCapturedLogEvent.isPresent()) {
                capturedLogEvent = lastCapturedLogEvent.get().thenLogged(level, regex, expectedMdcEntries.toArray(new ExpectedMdcEntry[0]));
            } else {
                capturedLogEvent = logCapture.assertLogged(level, regex, expectedMdcEntries.toArray(new ExpectedMdcEntry[0]));
            }
            FluentLogAssertion newFluentLogAssertion = new FluentLogAssertion(logCapture, Optional.of(capturedLogEvent));
            newFluentLogAssertion.expectedMdcEntries.addAll(FluentLogAssertion.this.expectedMdcEntries);
            return newFluentLogAssertion;
        }
    }
}
