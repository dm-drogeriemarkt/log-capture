package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * a JUnit 4 @Rule that can be used to capture log output. Use the appropriate constructor for unit/integration tests.
 */
public final class LogCapture implements TestRule { //should implement AfterEachCallback, BeforeEachCallback in JUnit 5

    private final boolean forUnitTest;
    private final List<String> capturedPackages;
    private CapturingAppender capturingAppender;
    private Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    /**
     * LogCapture for unit test - will filter out any log messages that are not caused by the test using the
     * message's call stack
     *
     * @return LogCapture for unit test
     */
    public static LogCapture forUnitTest() {
        return new LogCapture(true, Collections.emptyList());
    }

    /**
     * LocCapture for integration test - needs some packages (for example "de.dm") to define which logs should
     * be captured at DEBUG level. Other package's messages may also be captured depending on the log level
     * defined in the application
     *
     * @param capturedPackages packages whose logs should be captured at DEBUG level
     *
     * @return LogCapture for integration test
     */
    public static LogCapture forIntegrationTest(String... capturedPackages) {
        if (capturedPackages.length == 0) {
            throw new IllegalArgumentException("LogCapture must capture at least one package.");
        }
        return new LogCapture(false, Arrays.asList(capturedPackages));
    }

    private LogCapture(boolean forUnitTest, List<String> capturedPackages) {
        this.forUnitTest = forUnitTest;
        this.capturedPackages = capturedPackages;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                addAppender();
                try {
                    base.evaluate();
                } finally {
                    removeAppender();
                }
            }
        };
    }

    /**
     * Use this if you are not using LogCapture via JUnit's @Rule
     * <p>
     * For example, this may be used in a Method that is annotated with Cucumber's @Before annotation to start capturing.
     * In this case, make sure you also call {@link LogCapture#removeAppender()} in an @After method
     */
    public void addAppender() {
        capturingAppender = new CapturingAppender(rootLogger.getLoggerContext(), forUnitTest, capturedPackages);
        if (capturedPackages.size() == 0) {
            rootLogger.getLoggerContext().getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.DEBUG);
        } else {
            capturedPackages.forEach(packageName ->
                    rootLogger.getLoggerContext().getLogger(packageName).setLevel(Level.DEBUG)
            );
        }
        rootLogger.addAppender(capturingAppender);
    }

    /**
     * Use this if you are not using LogCapture via JUnit's @Rule
     * <p>
     * For example, this may be used in a Method that is annotated with Cucumber's @After annotation to start capturing.
     */
    public void removeAppender() {
        rootLogger.detachAppender(capturingAppender);
    }

    /**
     * assert that some message has been logged
     *
     * @param level expected log level
     * @param regex regex to match formatted log message
     * @param expectedMdcEntries expected MDC entries, see @{@link ExpectedMdcEntry}
     *
     * @return a LastCapturedLogEvent from which .thenLogged(...) can be called to assert if things have been logged in a specific order
     */
    public LastCapturedLogEvent assertLogged(Level level, String regex, ExpectedMdcEntry... expectedMdcEntries) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        return assertLogged(level, regex, 0, caller, expectedMdcEntries);
    }

    private LastCapturedLogEvent assertLogged(Level level, String regex, int index, StackTraceElement caller, ExpectedMdcEntry... expectedMdcEntries) {
        if (capturingAppender == null) {
            throw new IllegalStateException("capuringAppender is null. Please make sure that either LogCapture is used with a @Rule annotation or that addAppender is called manually.");
        }
        Integer foundAtIndex = capturingAppender.whenCapturedNext(level, regex, index, caller, expectedMdcEntries);
        return new LastCapturedLogEvent(foundAtIndex, caller);
    }

    /**
     * Helper to allow for comfortable assertions to check the order in which things are logged
     */
    @RequiredArgsConstructor
    public class LastCapturedLogEvent {
        private final int index;
        private final StackTraceElement caller;

        /**
         * assert that something has been logged after this event
         *
         * @param level expected log level
         * @param regex regex to match formatted log message
         * @param expectedMdcEntries expected MDC entries, see @{@link ExpectedMdcEntry}
         *
         * @return another LastCapturedLogEvent - for obvious reasons
         */
        public LastCapturedLogEvent thenLogged(Level level, String regex, ExpectedMdcEntry... expectedMdcEntries) {
            return assertLogged(level, regex, index + 1, caller, expectedMdcEntries);
        }
    }

}
