package de.dm.infrastructure.logcapture;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify which packages should be captured when using {@link LogCaptureExtension}.
 *
 * <p>If this annotation is not present, the test class's own package is captured by default.
 *
 * <p>Example:
 * <pre>{@code
 * @ExtendWith(LogCaptureExtension.class)
 * @LogCapturePackages({"com.example.app", "com.example.lib"})
 * class MyTest {
 *     @Test
 *     void myTest(LogCapture logCapture) {
 *         // ...
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogCapturePackages {

    /**
     * Packages whose log output should be captured. Sub-packages are included automatically.
     *
     * @return package names to capture
     */
    String[] value();
}
