package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * a JUnit 5 Extension that can be used to capture log output. Use the appropriate constructor for unit/integration tests.
 */
public final class LogCapture implements BeforeEachCallback, AfterEachCallback {

    final Set<String> capturedPackages;
    CapturingAppender capturingAppender;
    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    private HashMap<String, Level> originalLogLevels = null;

    /**
     * Instantiate LogCapture with some packages (for example "de.dm") to define which logs should
     * be captured at DEBUG level. Sub-package's messages will also be captured depending on the log level
     * defined in the application
     * <p>
     * e.g. if you define "de.dm" as the captured package, messages from "de.dm.foo.bar" will also ve captured
     *
     * @param capturedPackage package whose logs should be captured at DEBUG level
     * @param capturedPackages more packages whose logs should be captured at DEBUG level
     *
     * @return LogCapture instance to be used in test
     */
    public static LogCapture forPackages(String capturedPackage, String... capturedPackages) {
        HashSet<String> capturedPackageSet = new HashSet<>(Arrays.asList(capturedPackages));
        capturedPackageSet.add(capturedPackage);
        return new LogCapture(capturedPackageSet);
    }

    /**
     * Instantiate LogCapture with the current test's package for capturing. Sub-packages' messages
     * will also be captured as in {@link LogCapture#forPackages(String, String...)}.
     *
     * @return LogCapture instance to be used in test
     */
    public static LogCapture forCurrentPackage() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String className = caller.getClassName();
        String packageName = className.substring(0, className.lastIndexOf("."));
        return LogCapture.forPackages(packageName);
    }

    private LogCapture(Set<String> capturedPackages) {
        this.capturedPackages = capturedPackages;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        addAppenderAndSetLogLevelToTrace();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        removeAppenderAndResetLogLevel();
    }

    /**
     * Use this if you are not using LogCapture via JUnit's @Rule
     * <p>
     * For example, this may be used in a Method that is annotated with Cucumber's @Before annotation to start capturing.
     * In this case, make sure you also call {@link LogCapture#removeAppenderAndResetLogLevel()} in an @After method
     */
    public void addAppenderAndSetLogLevelToTrace() {
        capturingAppender = new CapturingAppender(rootLogger.getLoggerContext(), capturedPackages);
        rootLogger.addAppender(capturingAppender);
        setLogLevelToTrace();
    }

    /**
     * delegates to {@link LogCapture#addAppenderAndSetLogLevelToTrace()} for compatibility
     *
     * @deprecated (because log level actually needs to be set to TRACE, not DEBUG)
     */
    @Deprecated
    public void addAppenderAndSetLogLevelToDebug() {
        addAppenderAndSetLogLevelToTrace();
    }

    private void setLogLevelToTrace() {
        if (originalLogLevels != null) {
            throw new IllegalStateException("LogCapture.addAppenderAndSetLogLevelToTrace() should not be called only once or after calling removeAppenderAndResetLogLevel() again.");
        }
        originalLogLevels = new HashMap<>();
        capturedPackages.forEach(packageName -> {
                    Logger packageLogger = rootLogger.getLoggerContext().getLogger(packageName);
                    originalLogLevels.put(packageName, packageLogger.getLevel());
                    rootLogger.getLoggerContext().getLogger(packageName).setLevel(Level.TRACE);
                }
        );
    }

    private void resetLogLevel() {
        if (originalLogLevels == null) {
            throw new IllegalStateException("LogCapture.resetLogLevel() should only be called after calling addAppenderAndSetLogLevelToTrace()");
        }
        capturedPackages.forEach(packageName ->
                rootLogger.getLoggerContext().getLogger(packageName).setLevel(originalLogLevels.get(packageName))
        );
        originalLogLevels = null;
    }

    /**
     * Use this if you are not using LogCapture via JUnit's @Rule
     * <p>
     * For example, this may be used in a Method that is annotated with Cucumber's @After annotation to start capturing.
     */
    public void removeAppenderAndResetLogLevel() {
        rootLogger.detachAppender(capturingAppender);
        resetLogLevel();
    }

    /**
     * assert that some message has been logged
     *
     * @param level expected log level
     * @param regex regex to match formatted log message (with Pattern.DOTALL and Pattern.MULTILINE)
     * @param expectedMdcEntries expected MDC entries, see @{@link ExpectedMdcEntry}
     *
     * @return a LastCapturedLogEvent from which .thenLogged(...) can be called to assert if things have been logged in a specific order
     *
     * @throws AssertionError if the expected log message has not been logged
     * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
     */
    @Deprecated
    public LastCapturedLogEvent assertLogged(Level level, String regex, ExpectedMdcEntry... expectedMdcEntries) {
        return assertLogged(level, regex, null, expectedMdcEntries);
    }

    @Deprecated
    private LastCapturedLogEvent assertLogged(Level level, String regex, LastCapturedLogEvent lastCapturedLogEvent, ExpectedMdcEntry... expectedMdcEntries) {
        if (capturingAppender == null) {
            throw new IllegalStateException("capturingAppender is null. " +
                    "Please make sure that either LogCapture is used with a @Rule annotation or that addAppenderAndSetLogLevelToTrace is called manually.");
        }

        int startIndex = lastCapturedLogEvent == null ? 0 : lastCapturedLogEvent.lastAssertedLogMessageIndex + 1;
        int numberOfAssertedLogMessages = lastCapturedLogEvent == null ? 1 : lastCapturedLogEvent.numberOfAssertedLogMessages + 1;

        List<LogEventMatcher> expectedMdcEntriesList = expectedMdcEntries != null ? Arrays.asList(expectedMdcEntries) : Collections.emptyList();

        Integer foundAtIndex = new LogAsserter(capturingAppender, new LinkedList<>()).assertCapturedNext(Optional.of(level), Optional.of(regex), startIndex, expectedMdcEntriesList);

        return new LastCapturedLogEvent(foundAtIndex, numberOfAssertedLogMessages);
    }

    /**
     * assert that a certain expected message has been logged.
     *
     * <p>Example:
     * <pre>{@code
     * logCapture.assertLogged(info("hello world"));
     * }</pre>
     *
     * @param logExpectation description of the expected log message
     *
     * @return asserter that can be used to check if anything else has been logged
     *
     * @throws AssertionError if the expected log message has not been logged
     */
    public LogAsserter.NothingElseLoggedAsserter assertLogged(LogExpectation logExpectation) {
        return new LogAsserter(capturingAppender, new LinkedList<>())
                .assertLogged(logExpectation);
    }

    /**
     * assert that a certain expected message has not been logged.
     *
     * <p>Example:
     * <pre>{@code
     *  logCapture.assertNothingMatchingLogged(info("hello world"));
     * }</pre>
     *
     * @param logExpectations descriptions of log messages that should not occur
     *
     * @throws AssertionError if the expected log message has not been logged
     * @throws IllegalArgumentException if no LogExpectation is provided
     */
    public void assertNotLogged(LogExpectation... logExpectations) {
        new LogAsserter(capturingAppender, new LinkedList<>())
                .assertNotLogged(logExpectations);
    }

    /**
     * assert that multiple log messages have been logged in any order
     *
     * <p>Example:
     * <pre>{@code
     * logCapture.assertLoggedInAnyOrder(
     *     info("bye world"),
     *     warn("hello world")
     * ));
     * }</pre>
     *
     * @param logExpectations more descriptions of expected log messages
     *
     * @return asserter that can be used to check if anything else has been logged
     *
     * @throws AssertionError if any of the expected log message has not been logged or matching is imprecise (in case multiple expectations match the same message)
     * @throws IllegalArgumentException if less than two LogExpectations are provided
     */
    public LogAsserter.NothingElseLoggedAsserter assertLoggedInAnyOrder(LogExpectation... logExpectations) {
        return new LogAsserter(capturingAppender, new LinkedList<>())
                .assertLoggedInAnyOrder(logExpectations);
    }

    /**
     * assert that multiple log messages have been logged in an expected order
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .assertLoggedInOrder(
     *         info("hello world"),
     *         warn("bye world")
     *     ));
     * }</pre>
     *
     * @param logExpectations descriptions of expected log messages, in order
     *
     * @return asserter that can be used to check if anything else has been logged
     *
     * @throws AssertionError if any of the expected log message has not been logged or have been logged in the wrong order
     * @throws IllegalArgumentException if less than two LogExpectations are provided
     */
    public LogAsserter.NothingElseLoggedAsserter assertLoggedInOrder(LogExpectation... logExpectations) {
        return new LogAsserter(capturingAppender, new LinkedList<>())
                .assertLoggedInOrder(logExpectations);
    }


    /**
     * set up additional log matchers describing aspects that all asserted log messages should match (for example MDC content)
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .with(
     *         mdc("key", "value"))
     *     .assertLoggedInAnyOrder(
     *         info("hello world"),
     *         warn("bye world")
     *     ));
     * }</pre>
     *
     * @param logEventMatchers log event matchers describing expectations
     *
     * @return an asserter to assert log messages with the described additional expectations
     *
     * @throws IllegalArgumentException if no LogEventMatcher is provided
     */
    public LogAsserter with(LogEventMatcher... logEventMatchers) {
        if (logEventMatchers.length < 1) {
            throw new IllegalArgumentException("with() needs at least one LogEventMatcher");
        }

        return new LogAsserter(capturingAppender, Arrays.asList(logEventMatchers));
    }

    /**
     * Helper to allow for comfortable assertions to check the order in which things are logged
     *
     * @deprecated in favor of the new API
     */
    @RequiredArgsConstructor
    @Deprecated
    public class LastCapturedLogEvent {
        private final int lastAssertedLogMessageIndex;
        private final int numberOfAssertedLogMessages;

        /**
         * assert that something has been logged after this event
         *
         * @param level expected log level
         * @param regex regex to match formatted log message
         * @param expectedMdcEntries expected MDC entries, see @{@link ExpectedMdcEntry}
         *
         * @return another LastCapturedLogEvent - for obvious reasons
         *
         * @throws AssertionError if the expected log message has not been logged
         * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
         */
        @Deprecated
        public LastCapturedLogEvent thenLogged(Level level, String regex, ExpectedMdcEntry... expectedMdcEntries) {
            return assertLogged(level, regex, this, expectedMdcEntries);
        }

        /**
         * assert that nothing else has been logged except for the asserted log messages
         *
         * @throws AssertionError if something else has been logged
         * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
         */
        @Deprecated
        public void assertNothingElseLogged() {
            if (capturingAppender.loggedEvents.size() > numberOfAssertedLogMessages) {
                throw new AssertionError("There have been other log messages than the asserted ones.");
            }
        }
    }

    /**
     * prepare the assertion of log messages with MDC contents
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .withMdcForAll("key", "value")
     *     .info().assertLogged("hello world")
     *     .warn().assertLogged("bye world"));
     * }</pre>
     *
     * @param key MDC key
     * @param regex regular expression describing the MDC value
     *
     * @return FluentLogAssertion to assert the messages with MDC
     *
     * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
     */
    @Deprecated
    public FluentLogAssertion withMdcForAll(String key, String regex) {
        return new FluentLogAssertion(this, Optional.empty())
                .withMdcForAll(key, regex);
    }

    /**
     * prepare the assertion of a logged error message
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .error().assertLogged("hello world")
     * }</pre>
     *
     * @return FluentLogAssertion to assert an error message
     *
     * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
     */
    @Deprecated
    public FluentLogAssertion.ConfiguredLogAssertion error() {
        return new FluentLogAssertion(this, Optional.empty())
                .error();
    }

    /**
     * prepare the assertion of a logged warn message
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .warn().assertLogged("hello world")
     * }</pre>
     *
     * @return FluentLogAssertion to assert an warn message
     *
     * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
     */
    @Deprecated
    public FluentLogAssertion.ConfiguredLogAssertion warn() {
        return new FluentLogAssertion(this, Optional.empty())
                .warn();
    }

    /**
     * prepare the assertion of a logged info message
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .info().assertLogged("hello world")
     * }</pre>
     *
     * @return FluentLogAssertion to assert an info message
     *
     * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
     */
    @Deprecated
    public FluentLogAssertion.ConfiguredLogAssertion info() {
        return new FluentLogAssertion(this, Optional.empty())
                .info();
    }

    /**
     * prepare the assertion of a logged debug message
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .debug().assertLogged("hello world")
     * }</pre>
     *
     * @return FluentLogAssertion to assert an debug message
     *
     * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
     */
    @Deprecated
    public FluentLogAssertion.ConfiguredLogAssertion debug() {
        return new FluentLogAssertion(this, Optional.empty())
                .debug();
    }

    /**
     * prepare the assertion of a logged trace message
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .trace().assertLogged("hello world")
     * }</pre>
     *
     * @return FluentLogAssertion to assert an trace message
     *
     * @deprecated use the new assertion methods (withMdc(), withException(), assertLogged(), assertLoggedInOrder()) instead
     */
    @Deprecated
    public FluentLogAssertion.ConfiguredLogAssertion trace() {
        return new FluentLogAssertion(this, Optional.empty())
                .trace();
    }
}
