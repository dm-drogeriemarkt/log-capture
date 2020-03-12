package de.dm.infrastructure.logcapture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class CapturingAppenderIntegrationTest {

    private final String TEST_KEY = "test_key";
    private final String OTHER_KEY = "test_key_2";

    @Test
    public void containsMdcEntries() {
        ExpectedMdcEntry expectedMdcEntry1 = ExpectedMdcEntry.withMdc(TEST_KEY, "test value");
        ExpectedMdcEntry expectedMdcEntry2 = ExpectedMdcEntry.withMdc(OTHER_KEY, "good value");
        ExpectedMdcEntry[] expectedMdcEntries = new ExpectedMdcEntry[]{expectedMdcEntry1, expectedMdcEntry2};

        Map<String, String> mdcContents = new HashMap<>();
        mdcContents.put(TEST_KEY, "this is a test value, cool!");
        mdcContents.put(OTHER_KEY, "this is a good value, cool!");

        Assertions.assertTrue(CapturingAppender.containsMdcEntries(mdcContents, expectedMdcEntries));
    }

    @Test
    public void nullEntriesShouldNotThrowNullPointerException() {
        Map<String, String> mdcContents = new HashMap<>();
        Assertions.assertTrue(CapturingAppender.containsMdcEntries(mdcContents, null));
    }
}
