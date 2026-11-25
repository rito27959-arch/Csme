package com.csme.server.unit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Garde-fous d'architecture : les couches ne se mélangent pas.
 * Écrits en tests JUnit classiques (ArchRule.check) pour ne pas dépendre du moteur JUnit d'ArchUnit.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.csme.server");
    }

    @Test
    void domainIsIndependent() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "com.csme.server.api..", "..infrastructure..",
                        "org.springframework..")
                .check(classes);
    }

    @Test
    void controllersDoNotTouchPersistence() {
        noClasses().that().resideInAPackage("com.csme.server.api..")
                .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.persistence..", "..domain.model..")
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnApi() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("com.csme.server.api..")
                .check(classes);
    }
}
