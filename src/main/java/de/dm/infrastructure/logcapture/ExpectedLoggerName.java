package de.dm.infrastructure.logcapture;

import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

public class ExpectedLoggerName implements LogEventMatcher {
    private final Pattern expectedName;
    private final String inputRegex;

    ExpectedLoggerName(String loggerNameRegex) {
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
    public String getMatcherDescription() {
        return "logger name";
    }

    public static ExpectedLoggerName logger(String loggerNameRegex) {
        return new ExpectedLoggerName(loggerNameRegex);
    }
}
