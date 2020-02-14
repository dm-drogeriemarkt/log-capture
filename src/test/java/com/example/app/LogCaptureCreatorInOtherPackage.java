package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;

public class LogCaptureCreatorInOtherPackage {
    public static LogCapture getLogCaptureFromCurrentPackage() {
        return LogCapture.forCurrentPackage();
    }
}
