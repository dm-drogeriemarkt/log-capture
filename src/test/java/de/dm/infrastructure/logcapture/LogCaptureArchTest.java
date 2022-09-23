package de.dm.infrastructure.logcapture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packagesOf = LogCapture.class, importOptions = DoNotIncludeTests.class)
public class LogCaptureArchTest {
    @ArchTest
    static ArchRule niceApiForExpectations = classes()
            .that()
            .haveSimpleNameStartingWith("Expected")
            .and()
            .haveSimpleNameNotStartingWith("ExpectedException")// because somehow ArchUnit has hiccups in this class, although it only has private constructors
            .should()
            .haveOnlyPrivateConstructors()
            .because("log expectations should have easy-to-read builders instead of public constructors");
}
