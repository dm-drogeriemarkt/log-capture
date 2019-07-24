package de.dm.infrastructure.logcapture;

/**
 * implement this if you have custom requirements when matching MDC contents
 */
public interface MdcMatcher {

    /**
     * check if an actual MDC value matches the expectations
     *
     * @param mdcValue actual MDC value
     *
     * @return if the expectations are matched
     */
    boolean matches(String mdcValue);
}
