package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;

import static de.dm.infrastructure.logcapture.ExpectedException.exception;
import static de.dm.infrastructure.logcapture.ExpectedLoggerName.logger;
import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;
import static de.dm.infrastructure.logcapture.LogAssertion.debug;
import static de.dm.infrastructure.logcapture.LogAssertion.error;
import static de.dm.infrastructure.logcapture.LogAssertion.info;
import static de.dm.infrastructure.logcapture.LogAssertion.trace;
import static de.dm.infrastructure.logcapture.LogAssertion.warn;
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class ReadableApiTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void basicReadableApiSucceeds() {
        log.info("hello world", new IllegalArgumentException("shame on you", new NullPointerException("bah!")));

        logCapture.assertLogged(info("hello world"));
    }

    @Test
    void allLevelsWork() {
        log.error("first error");
        log.error("second error");
        log.warn("first warn");
        log.warn("second warn");
        log.info("first info");
        log.info("second info");
        log.debug("first debug");
        log.debug("second debug");
        log.trace("first trace");
        log.trace("second trace");

        logCapture.assertLoggedInOrder(
                error("first error"),
                error("second error"));
        logCapture.assertLoggedInOrder(
                warn("first warn"),
                warn("second warn"));
        logCapture.assertLoggedInOrder(
                info("first info"),
                info("second info"));
        logCapture.assertLoggedInOrder(
                debug("first debug"),
                debug("second debug"));
        logCapture.assertLoggedInOrder(
                trace("first trace"),
                trace("second trace"));
    }

    @Nested
    class ExpectedExceptionWorks {
        @Test
        void expectedExceptionSucceeds() {
            log.warn("oh no!", new IllegalArgumentException("shame on you!", new NullPointerException("never use null")));

            logCapture.assertLogged(
                    warn("oh no!",
                            exception()
                                    .expectedMessageRegex("shame on you!")
                                    .expectedType(RuntimeException.class)
                                    .build()
                    ));
            logCapture.assertLogged(
                    warn("oh no!",
                            exception()
                                    .expectedType(IllegalArgumentException.class)
                                    .expectedCause(exception()
                                            .expectedType(NullPointerException.class)
                                            .expectedMessageRegex("never use null")
                                            .build())
                                    .build()
                    ));
        }

        @Test
        void expectedExceptionFails() {
            log.info("without exception");
            log.warn("oh no!", new IllegalArgumentException("this is illegal", new NullPointerException("never be null!")));

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            warn("oh no!",
                                    exception()
                                            .expectedMessageRegex("a message never used")
                                            .expectedType(RuntimeException.class)
                                            .build()
                            )));

            assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected Exception: Level: WARN, Regex: \"oh no!\"" +
                    lineSeparator() + "  expected exception: message (regex): \"a message never used\" type: java.lang.RuntimeException" +
                    lineSeparator() + "  actual exception: message: \"this is illegal\", message: java.lang.IllegalArgumentException, cause: (message: \"never be null!\", message: java.lang.NullPointerException)" +
                    lineSeparator());

            AssertionError withoutExceptionAssertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("without exception",
                                    exception()
                                            .expectedMessageRegex("a message never used")
                                            .expectedType(RuntimeException.class)
                                            .build()
                            )));

            assertThat(withoutExceptionAssertionError).hasMessage("Expected log message has occurred, but never with the expected Exception: Level: INFO, Regex: \"without exception\"" +
                    lineSeparator() + "  expected exception: message (regex): \"a message never used\" type: java.lang.RuntimeException" +
                    lineSeparator() + "  actual exception: (null)" +
                    lineSeparator());
        }
    }

    @Nested
    class ExpectedLogger {
        @Test
        void expectedLoggerSucceeds() {
            log.info("hello world");
            log.warn("bye world");

            logCapture.assertLogged(
                    warn("bye world",
                            logger("ReadableApiTest$"))
            );

            logCapture.with(
                    logger("ReadableApiTest$")
            ).assertLoggedMessage(
                    warn("bye world"),
                    info("hello world")
            );

            logCapture.with(
                    logger("^com.example.app.ReadableApiTest$")
            ).assertLoggedMessage(
                    info("hello world"),
                    warn("bye world")
            );
        }

        @Test
        void expectedLoggerFails() {
            log.info("hello world");
            log.warn("bye world");

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("hello world"),
                            warn("bye world",
                                    logger("WrongLogger$"))
                    ));

            assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected logger name: Level: WARN, Regex: \"bye world\"" +
                    lineSeparator() + "  expected logger name (regex): \"WrongLogger$\"" +
                    lineSeparator() + "  actual logger name: \"com.example.app.ReadableApiTest\"" +
                    lineSeparator());
        }
    }

    @Nested
    class ExpectedMdc {
        @Test
        void singleMdcSucceeds() {
            log.info("hello world");
            MDC.put("key", "value");
            log.warn("bye world");
            MDC.clear();

            logCapture.assertLogged(
                    info("hello world"),
                    warn("bye world",
                            mdc("key", "value"))
            );
        }

        @Test
        void singleMdcFails() {
            MDC.put("key", "value");
            log.info("hello world");
            MDC.clear();
            log.warn("bye world");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLogged(
                            info("hello world"),
                            warn("bye world",
                                    mdc("key", "value"))
                    ));

            assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                    lineSeparator() + "  captured message: \"bye world\"" +
                    lineSeparator() + "  expected MDC key: key" +
                    lineSeparator() + "  expected MDC value: \".*value.*\"" +
                    lineSeparator() + "  captured MDC values:" +
                    lineSeparator());
        }

        @Test
        void mdcForAllFails() {
            MDC.put("key", "value");
            log.info("hello world");
            MDC.clear();
            log.warn("bye world");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .with(
                                    mdc("key", "value"))
                            .assertLoggedMessage(
                                    info("hello world"),
                                    warn("bye world")
                            ));

            assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                    lineSeparator() + "  captured message: \"bye world\"" +
                    lineSeparator() + "  expected MDC key: key" +
                    lineSeparator() + "  expected MDC value: \".*value.*\"" +
                    lineSeparator() + "  captured MDC values:" +
                    lineSeparator());
        }

        @Test
        void mdcForAllSucceeds() {
            MDC.put("key", "value");
            log.info("hello world");
            log.warn("bye world");
            MDC.clear();

            logCapture
                    .with(
                            mdc("key", "value"))
                    .assertLoggedMessage(
                            info("hello world"),
                            warn("bye world"));
        }

        @Test
        void mdcForAllAndSingleMdcFails() {
            MDC.put("key", "value");
            MDC.put("another_key", "another_value");
            log.info("hello world");
            MDC.remove("another_key");
            log.warn("bye world");
            MDC.clear();

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .with(
                                    mdc("key", "value"))
                            .assertLoggedMessage(
                                    info("hello world"),
                                    warn("bye world",
                                            mdc("another_key", "another_value"))
                            ));

            assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                    lineSeparator() + "  captured message: \"bye world\"" +
                    lineSeparator() + "  expected MDC key: key" +
                    lineSeparator() + "  expected MDC value: \".*value.*\"" +
                    lineSeparator() + "  captured MDC values:" +
                    lineSeparator() + "    key: \"value\"" +
                    lineSeparator() + "Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                    lineSeparator() + "  captured message: \"bye world\"" +
                    lineSeparator() + "  expected MDC key: another_key" +
                    lineSeparator() + "  expected MDC value: \".*another_value.*\"" +
                    lineSeparator() + "  captured MDC values:" +
                    lineSeparator() + "    key: \"value\"" +
                    lineSeparator());
        }

        @Test
        void mdcForAllAndSingleMdcSucceeds() {
            MDC.put("key", "value");
            log.info("hello world");
            MDC.put("another_key", "another_value");
            log.warn("bye world");
            MDC.remove("another_key");
            log.error("hello again");
            MDC.clear();

            logCapture
                    .with(
                            mdc("key", "value")
                    )
                    .assertLoggedInOrder(
                            info("hello world"),
                            warn("bye world",
                                    mdc("another_key", "another_value")),
                            error("hello again"));
        }
    }

    @Nested
    class InOrder {
        @Test
        void assertionWithoutOrderSucceeds() {
            log.info("hello 1");
            log.info("hello 2");
            log.info("hello 3");

            logCapture.assertLogged(
                    info("hello 3"),
                    info("hello 1"),
                    info("hello 2")
            );
        }

        @Test
        void assertionWithoutOrderFails() {
            log.info("hello 1");
            log.info("hello 3");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLogged(
                            info("hello 3"),
                            info("hello 1"),
                            info("hello 2")
                    ));

            assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: INFO, Regex: \"hello 2\"");
        }

        @Test
            // this behavior is necessary because
            // 1. otherwise nothingElseLogged() would be a lot more complicated
            // 2. is is reasonable to assume that is it not the user's intention to verify the same message twice
        void assertionWithoutOrderMatchingSameMessageFails() {
            log.info("hello 1");
            log.info("hello 3");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLogged(
                            info("hello"),
                            info("1")
                    ));

            assertThat(assertionError).hasMessage("Imprecise matching: Two log assertions have matched the same message. " +
                    "Use more precise matching or in-order matching. " +
                    "(First match: Level: INFO, Regex: \"hello\" | Second match: Level: INFO, Regex: \"1\"");
        }

        @Test
        void assertionWithOrderSucceeds() {
            log.info("hello 1");
            log.info("hello 2");
            log.info("hello 3");

            logCapture.assertLoggedInOrder(
                    info("hello 1"),
                    info("hello 2"),
                    info("hello 3")
            );
        }

        @Test
        void assertionWithOrderFails() {
            log.info("hello 1");
            log.info("hello 2");
            log.info("hello 3");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLoggedInOrder(
                            info("hello 1"),
                            info("hello 3"),
                            info("hello 2")
                    ));

            assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: INFO, Regex: \"hello 2\""); //TODO: output that it has occurred before, out of order
        }

        @Test
        void nothingElseLoggedInOrderSucceeds() {
            log.info("hello world");
            log.info("hello universe");

            logCapture
                    .assertLoggedInOrder(
                            info("hello world"),
                            info("hello universe"))
                    .assertNothingElseLogged();
        }

        @Test
        void nothingElseLoggedInOrderFails() {
            log.info("hello world");
            log.info("hello universe");
            log.info("hello multiverse");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .assertLoggedInOrder(
                                    info("hello world"),
                                    info("hello universe"))
                            .assertNothingElseLogged());

            assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
        }

        @Test
        void nothingElseLoggedOutOfOrderSucceeds() {
            log.info("hello world");
            log.info("hello universe");

            logCapture
                    .assertLogged(
                            info("hello universe"),
                            info("hello world"))
                    .assertNothingElseLogged();
        }

        @Test
        void nothingElseLoggedOutOfOrderFails() {
            log.info("hello world");
            log.info("hello multiverse");
            log.info("hello universe");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .assertLogged(
                                    info("hello universe"),
                                    info("hello world"))
                            .assertNothingElseLogged());

            assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
        }
    }

    //TODO: check if all necessary MDC combinations are tested


}
