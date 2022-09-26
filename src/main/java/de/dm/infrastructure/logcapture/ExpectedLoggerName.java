package de.dm.infrastructure.logcapture;

import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

/**
 * define expected logger from which a message is supposed to be logged
 */
public final class ExpectedLoggerName implements LogEventMatcher {
    private final Pattern expectedName;
    private final String inputRegex;

    private ExpectedLoggerName(String loggerNameRegex) {
        inputRegex = loggerNameRegex;
        expectedName = Pattern.compile(".*" + loggerNameRegex + ".*");
    }

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        return expectedName.matcher(loggedEvent.getLoggerName()).matches();
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        return format("  expected logger name (regex): \"%s\"", inputRegex) +
                lineSeparator() +
                format("  actual logger name: \"%s\"", loggedEvent.getLoggerName());
    }

    @Override
    public String getMatcherTypeDescription() {
        return "logger name";
    }

    @Override
    public String getMatcherDetailDescription() {
        return format("logger name (regex): \"%s\"", inputRegex);
    }

    /**
     * use this in a log expectation to verify that something has been logged from a certain logger
     *
     * @param loggerNameRegex regular expression that matches the expected logger. Will be padded with .* - so for an exact match, use ^my.expected.logger$
     *
     * @return expected logger name to use in log expectation
     */
    public static ExpectedLoggerName logger(String loggerNameRegex) {
        return new ExpectedLoggerName(loggerNameRegex);
    }
}
