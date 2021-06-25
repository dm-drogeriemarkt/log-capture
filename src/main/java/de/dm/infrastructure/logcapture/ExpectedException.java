package de.dm.infrastructure.logcapture;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

@Slf4j
public class ExpectedException implements LogEventMatcher {
    private final Optional<String> expectedMessageRegex;
    private final Optional<Pattern> expectedMessage;
    private final Optional<Class<? extends Exception>> expectedType;
    private final Optional<ExpectedException> expectedCause;

    private ExpectedException(Optional<String> expectedMessageRegex, Optional<Class<? extends Exception>> expectedType, Optional<ExpectedException> expectedCause) {
        if (expectedMessageRegex.isPresent()) {
            this.expectedMessageRegex = expectedMessageRegex;
            expectedMessage = Optional.of(Pattern.compile(".*" + expectedMessageRegex.get() + ".*", Pattern.DOTALL + Pattern.MULTILINE));
        } else {
            this.expectedMessageRegex = Optional.empty();
            expectedMessage = Optional.empty();
        }
        this.expectedType = expectedType;
        this.expectedCause = expectedCause;
    }

    public static ExpectedExceptionBuilder exception() {
        return new ExpectedExceptionBuilder();
    }

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        return exceptionMatches(loggedEvent.getLoggedException(), Optional.of(this));
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        return format("  expected exception: %s", this) +
                lineSeparator() +
                format("  actual exception: %s", loggedExceptionToString(loggedEvent.getLoggedException()));
    }

    private static String loggedExceptionToString(Optional<LoggedEvent.LoggedException> optionalException) {
        if (!optionalException.isPresent()) {
            return "(null)";
        }

        LoggedEvent.LoggedException loggedException = optionalException.get();

        return format("message: \"%s\", message: %s%s",
                loggedException.getMessage(),
                loggedException.getType(),
                loggedException.getCause().isPresent() ? format(", cause: (%s)", loggedExceptionToString(loggedException.getCause())) : ""
        );
    }

    @Override
    public String getMatcherDescription() {
        return "Exception";
    }

    private static boolean exceptionMatches(Optional<LoggedEvent.LoggedException> optionalActualException, Optional<ExpectedException> optionalExpectedException) {
        if (!optionalActualException.isPresent() && optionalExpectedException.isPresent()) {
            return false;
        }
        if (!optionalExpectedException.isPresent()) {
            return true;
        }

        ExpectedException expectedException = optionalExpectedException.get();
        LoggedEvent.LoggedException loggedException = optionalActualException.get();

        return expectedMessageMatches(loggedException, expectedException) &&
                expectedTypeMatches(loggedException, expectedException) &&
                exceptionMatches(loggedException.getCause(), expectedException.expectedCause);
    }

    private static boolean expectedMessageMatches(LoggedEvent.LoggedException loggedException, ExpectedException expectedException) {
        return !expectedException.expectedMessage.isPresent() ||
                expectedException.expectedMessage.get().matcher(loggedException.getMessage()).matches();
    }

    private static boolean expectedTypeMatches(LoggedEvent.LoggedException loggedException, ExpectedException expectedException) {
        if (!expectedException.expectedType.isPresent()) {
            return true;
        }
        try {
            Class<?> actualType = Class.forName(loggedException.getType());
            return expectedException.expectedType.get().isAssignableFrom(actualType);
        } catch (ClassNotFoundException e) {
            return expectedException.expectedType.get().getCanonicalName().equals(loggedException.getType());
        }
    }

    @Override
    public String toString() {
        return format("%s%s%s",
                expectedMessageRegex.isPresent() ? format("message (regex): \"%s\"", expectedMessageRegex.get()) : "",
                expectedType.map(aClass -> " type: " + aClass.getCanonicalName()).orElse(""),
                expectedCause.map(cause -> format(" cause: (%s)", cause)).orElse("")
        );
    }

    public static class ExpectedExceptionBuilder {
        private String expectedMessageRegex;
        private Class<? extends Exception> expectedType;
        private ExpectedException expectedCause;

        private ExpectedExceptionBuilder() {
        }

        public ExpectedExceptionBuilder expectedMessageRegex(String expectedMessageRegex) {
            this.expectedMessageRegex = expectedMessageRegex;
            return this;
        }

        public ExpectedExceptionBuilder expectedType(Class<? extends Exception> expectedType) {
            this.expectedType = expectedType;
            return this;
        }

        public ExpectedExceptionBuilder expectedCause(ExpectedException expectedCause) {
            this.expectedCause = expectedCause;
            return this;
        }

        public ExpectedException build() {
            return new ExpectedException(
                    Optional.ofNullable(expectedMessageRegex),
                    Optional.ofNullable(expectedType),
                    Optional.ofNullable(expectedCause));
        }

        @Override
        public String toString() {
            return "ExpectedException.ExpectedExceptionBuilder(expectedMessageRegex=" + expectedMessageRegex + ", expectedType=" + expectedType + ", expectedCause=" + expectedCause + ")";
        }
    }
}