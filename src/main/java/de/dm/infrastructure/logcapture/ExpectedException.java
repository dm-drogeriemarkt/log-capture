package de.dm.infrastructure.logcapture;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

/**
 * define expected logger from which a message is supposed to be logged
 */
@SuppressWarnings("squid:S2166") //naming this Exception is OK
public final class ExpectedException implements LogEventMatcher {
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

    /**
     * use this to build an expected Exception to use in a log expectation to verify that something has been logged
     * with a certain Exception
     *
     * @return builder for expected exception
     */
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

    @Override
    public String getMatcherDetailDescription() {
        return format("Exception: %s", this);
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
    public String getMatcherTypeDescription() {
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

    /**
     * helper for building ExcpectedExceptions
     */
    public static final class ExpectedExceptionBuilder {
        private String expectedMessageRegex;
        private Class<? extends Exception> expectedType;
        private ExpectedException expectedCause;

        private ExpectedExceptionBuilder() {
        }

        /**
         * set an expected message that should be matched for an expected Exception
         *
         * @param expectedMessageRegex regular expression mathing an exception's message. Will be padded with .*
         *
         * @return the builder with the expected message set
         */
        public ExpectedExceptionBuilder expectedMessageRegex(String expectedMessageRegex) {
            this.expectedMessageRegex = expectedMessageRegex;
            return this;
        }

        /**
         * set an expected type for an Exception that should be matched for an expected Exception
         *
         * @param expectedType expected type of the exception. Subtypes will be matched, too.
         *
         * @return the builder with the expected exception type
         */
        public ExpectedExceptionBuilder expectedType(Class<? extends Exception> expectedType) {
            this.expectedType = expectedType;
            return this;
        }

        /**
         * set an expected cause for an Exception that should be matched for an expected Exception
         *
         * @param expectedCause ExpectedException that describes the expected cause
         *
         * @return the builder with the expected cause
         */
        public ExpectedExceptionBuilder expectedCause(ExpectedException expectedCause) {
            this.expectedCause = expectedCause;
            return this;
        }

        /**
         * builds the ExpectedException so that it can be used in a log expectation
         *
         * @return the built Exception expectation
         */
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
