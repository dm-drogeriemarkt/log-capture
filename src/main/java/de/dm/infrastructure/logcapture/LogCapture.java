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
import java.util.Optional;
import java.util.Set;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * a JUnit 4 @Rule that can be used to capture log output. Use the appropriate constructor for unit/integration tests.
 */
public final class LogCapture implements BeforeEachCallback, AfterEachCallback { //should implement AfterEachCallback, BeforeEachCallback in JUnit 5

    final Set<String> capturedPackages;
    private CapturingAppender capturingAppender;
    private Logger rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
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
     */
    public LastCapturedLogEvent assertLogged(Level level, String regex, ExpectedMdcEntry... expectedMdcEntries) {
        return assertLogged(level, regex, null, expectedMdcEntries);
    }

    private LastCapturedLogEvent assertLogged(Level level, String regex, LastCapturedLogEvent lastCapturedLogEvent, ExpectedMdcEntry... expectedMdcEntries) {
        if (capturingAppender == null) {
            throw new IllegalStateException("capuringAppender is null. " +
                    "Please make sure that either LogCapture is used with a @Rule annotation or that addAppenderAndSetLogLevelToTrace is called manually.");
        }

        Integer startIndex = lastCapturedLogEvent == null ? 0 : lastCapturedLogEvent.index + 1;
        int assertedLogMessages = lastCapturedLogEvent == null ? 1 : lastCapturedLogEvent.assertedLogMessages + 1;

        Integer foundAtIndex = capturingAppender.whenCapturedNext(level, regex, startIndex, expectedMdcEntries);

        return new LastCapturedLogEvent(foundAtIndex, assertedLogMessages);
    }

    /**
     * Helper to allow for comfortable assertions to check the order in which things are logged
     */
    @RequiredArgsConstructor
    public class LastCapturedLogEvent {
        private final int index;
        private final int assertedLogMessages;

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
         */
        public LastCapturedLogEvent thenLogged(Level level, String regex, ExpectedMdcEntry... expectedMdcEntries) {
            return assertLogged(level, regex, this, expectedMdcEntries);
        }

        /**
         * assert that nothing else has been logged except for the asserted log messages
         *
         * @throws AssertionError if something else has been logged
         */
        public void assertNothingElseLogged() {
            if (capturingAppender.getNumberOfLoggedMessages() > assertedLogMessages) {
                throw new AssertionError("There have been other log messages than the asserted ones.");
            }
        }
    }

    public FluentLogAssertion withMdcForAll(String key, String regex) {
        return new FluentLogAssertion(this, Optional.empty())
                .withMdcForAll(key, regex);
    }

    public FluentLogAssertion.ConfiguredLogAssertion error() {
        return new FluentLogAssertion(this, Optional.empty())
                .error();
    }

    public FluentLogAssertion.ConfiguredLogAssertion warn() {
        return new FluentLogAssertion(this, Optional.empty())
                .warn();
    }

    public FluentLogAssertion.ConfiguredLogAssertion info() {
        return new FluentLogAssertion(this, Optional.empty())
                .info();
    }

    public FluentLogAssertion.ConfiguredLogAssertion debug() {
        return new FluentLogAssertion(this, Optional.empty())
                .debug();
    }

    public FluentLogAssertion.ConfiguredLogAssertion trace() {
        return new FluentLogAssertion(this, Optional.empty())
                .trace();
    }
}
