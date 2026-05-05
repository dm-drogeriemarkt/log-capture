package de.dm.infrastructure.logcapture;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A JUnit 5 extension that enables declarative log capturing via {@code @ExtendWith}.
 *
 * <p>By default, logs from the test class's package (and sub-packages) are captured.
 * Use {@link LogCapturePackages} to capture logs from other packages instead.
 *
 * <p>Use {@link LogCapture#logCapture()} to access the active instance:
 * <pre>{@code
 * import static de.dm.infrastructure.logcapture.LogCapture.logCapture;
 *
 * @ExtendWith(LogCaptureExtension.class)
 * class MyTest {
 *     @Test
 *     void myTest() {
 *         log.info("hello");
 *         logCapture().assertLogged(info("hello"));
 *     }
 * }
 * }</pre>
 *
 * @see LogCapturePackages
 */
public final class LogCaptureExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Set<String> packages = determinePackages(testClass);
        LogCapture logCapture = LogCapture.forPackageSet(packages);
        logCapture.addAppenderAndSetLogLevelToTrace();
        LogCapture.setCurrent(logCapture);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        LogCapture logCapture = LogCapture.logCapture();
        LogCapture.clearCurrent();
        logCapture.removeAppenderAndResetLogLevel();
    }

    private static Set<String> determinePackages(Class<?> testClass) {
        Optional<LogCapturePackages> annotation =
                AnnotationSupport.findAnnotation(testClass, LogCapturePackages.class);
        if (annotation.isPresent()) {
            String[] values = annotation.get().value();
            if (values.length == 0) {
                throw new IllegalArgumentException(
                        "@LogCapturePackages requires at least one package name");
            }
            return new HashSet<>(Arrays.asList(values));
        }
        return Set.of(testClass.getPackage().getName());
    }
}
