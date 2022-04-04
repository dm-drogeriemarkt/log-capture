package de.dm.infrastructure.logcapture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packagesOf = LogCapture.class, importOptions = DoNotIncludeTests.class)
public class LogCaptureArchTest {
    @ArchTest
    static ArchRule logstashIsOnlyUsedInDelegate = classes()
            .that()
            .doNotHaveFullyQualifiedName(ExpectedKeyValueLogstashDelegate.class.getCanonicalName())
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackage("net.logstash.logback..");
}
