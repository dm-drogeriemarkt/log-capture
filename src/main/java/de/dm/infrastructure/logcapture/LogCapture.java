package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * a JUnit 4 @Rule that can be used to capture log output. Use the appropriate constructor for unit/integration tests.
 */
public final class LogCapture implements BeforeEachCallback, AfterEachCallback { //should implement AfterEachCallback, BeforeEachCallback in JUnit 5

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
     * @deprecated (because log level actually needs to be set to TRACE, not DEBUG)
     * delegates to {@link LogCapture#addAppenderAndSetLogLevelToTrace()} for compatibility
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

        Integer foundAtIndex = new LogAsserter(capturingAppender, new LinkedList<>()).assertCapturedNext(level, regex, startIndex, Arrays.asList(expectedMdcEntries));

        return new LastCapturedLogEvent(foundAtIndex, numberOfAssertedLogMessages);
    }

    public LogAsserter.NothingElseLoggedAsserter assertLogged(LogAssertion logAssertion, LogAssertion... moreLogAssertions) {
        return new LogAsserter(capturingAppender, new LinkedList<>())
                .assertLoggedMessage(logAssertion, moreLogAssertions);
    }

    public LogAsserter.NothingElseLoggedAsserter assertLoggedInOrder(LogAssertion logAssertion, LogAssertion nextLogAssertion, LogAssertion... nextLogAssertions) {
        return new LogAsserter(capturingAppender, new LinkedList<>())
                .assertLoggedInOrder(logAssertion, nextLogAssertion, nextLogAssertions);
    }

    public LogAsserter with(LogEventMatcher logEventMatcher, LogEventMatcher... moreLogEventMatchers) {
        LinkedList<LogEventMatcher> logEventMatchers = new LinkedList<>();
        logEventMatchers.add(logEventMatcher);
        logEventMatchers.addAll(Arrays.asList(moreLogEventMatchers));
        return new LogAsserter(capturingAppender, logEventMatchers);
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
     * @param key MDC key
     * @param regex regular expression describing the MDC value
     *
     * <p>Example:
     * <pre>{@code
     * logCapture
     *     .withMdcForAll("key", "value")
     *     .info().assertLogged("hello world")
     *     .warn().assertLogged("bye world"));
     * }</pre>
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
