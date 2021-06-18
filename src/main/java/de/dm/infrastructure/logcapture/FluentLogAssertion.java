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

/**
 * Helper class for fluent log assertions. Use this via {@link LogCapture#info()} et al. or {@link LogCapture#withMdcForAll(String, String)}
 *
 * @deprecated in favor or the new assertion style
 */
@RequiredArgsConstructor(access = PACKAGE)
@Deprecated
public final class FluentLogAssertion {
    private final LogCapture logCapture;
    private final Optional<LastCapturedLogEvent> lastCapturedLogEvent;
    private final List<ExpectedMdcEntry> expectedMdcEntries = new LinkedList<>();

    /**
     * prepare the assertion of log messages with MDC contents
     *
     * @param key MDC key
     * @param regex regular expression describing the MDC value
     *
     * @return FluentLogAssertion to assert the messages with MDC
     */
    public FluentLogAssertion withMdcForAll(String key, String regex) {
        FluentLogAssertion newAssertion = new FluentLogAssertion(logCapture, lastCapturedLogEvent);
        newAssertion.expectedMdcEntries.addAll(expectedMdcEntries);
        newAssertion.expectedMdcEntries.add(ExpectedMdcEntry.withMdc(key, regex));

        return newAssertion;
    }

    /**
     * prepare the assertion of a logged warn message
     *
     * @return FluentLogAssertion to assert an warn message
     */
    public ConfiguredLogAssertion warn() {
        return new ConfiguredLogAssertion(WARN);
    }

    /**
     * prepare the assertion of a logged error message
     *
     * @return FluentLogAssertion to assert an error message
     */
    public ConfiguredLogAssertion error() {
        return new ConfiguredLogAssertion(ERROR);
    }

    /**
     * prepare the assertion of a logged info message
     *
     * @return FluentLogAssertion to assert an info message
     */
    public ConfiguredLogAssertion info() {
        return new ConfiguredLogAssertion(INFO);
    }

    /**
     * prepare the assertion of a logged debug message
     *
     * @return FluentLogAssertion to assert an debug message
     */
    public ConfiguredLogAssertion debug() {
        return new ConfiguredLogAssertion(DEBUG);
    }

    /**
     * prepare the assertion of a logged trace message
     *
     * @return FluentLogAssertion to assert an trace message
     */
    public ConfiguredLogAssertion trace() {
        return new ConfiguredLogAssertion(TRACE);
    }

    /**
     * assert that nothing else has been logged
     */
    public void assertNothingElseLogged() {
        LastCapturedLogEvent presentEvent = lastCapturedLogEvent.orElseThrow(() ->
                new IllegalStateException("assertNothingElseLogged() must be called with a previous log assertion"));
        presentEvent.assertNothingElseLogged();
    }

    /**
     * Helper class for fluent log assertions. Use this via {@link LogCapture#info()} et al. or {@link LogCapture#withMdcForAll(String, String)}
     *
     * @deprecated in favor of the new api
     */
    @Deprecated
    @RequiredArgsConstructor(access = PRIVATE)
    public class ConfiguredLogAssertion {
        private final Level level;
        private final List<ExpectedMdcEntry> expectedMdcEntries = new LinkedList<>(FluentLogAssertion.this.expectedMdcEntries);

        /**
         * configure the next message assertion to also assert MDC contents
         *
         * @param key MDC key
         * @param regex regular expression describing the MDC value
         *
         * @return pre-configured assertion
         */
        public ConfiguredLogAssertion withMdc(String key, String regex) {
            ConfiguredLogAssertion newAssertion = new ConfiguredLogAssertion(level);
            newAssertion.expectedMdcEntries.addAll(expectedMdcEntries);
            newAssertion.expectedMdcEntries.add(ExpectedMdcEntry.withMdc(key, regex));

            return newAssertion;
        }

        /**
         * assert that a message has been logged, depending on previous configuration
         *
         * @param regex regex to match formatted log message (with Pattern.DOTALL and Pattern.MULTILINE)
         *
         * @return configuration to allow further assertions
         */
        public FluentLogAssertion assertLogged(String regex) {
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
