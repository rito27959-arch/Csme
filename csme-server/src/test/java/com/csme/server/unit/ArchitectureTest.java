package com.csme.server.unit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Garde-fous d'architecture : les couches ne se mélangent pas. */
@AnalyzeClasses(packages = "com.csme.server", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsIndependent = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..api..", "..infrastructure..",
                    "org.springframework..");

    @ArchTest
    static final ArchRule controllersDoNotTouchPersistence = noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.persistence..", "..domain.model..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnApi = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..api..");
}
