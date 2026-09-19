package studio.quedena.template.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("studio.quedena.template");
    }

    // Regla 1 — El dominio es puro (sin framework)
    @Test
    void domainShouldNotDependOnFrameworks() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
                .check(importedClasses);
    }

    // Regla 3 — El límite del Bounded Context: un BC solo entra a otro por su
    // interfaces.acl (fachada OHS) o su domain.model.events (evento de integración).
    // `shared` no es un BC — es infraestructura transversal (ver Regla 5): cualquier
    // BC puede depender de `shared`, así que esa dirección se ignora aquí también.
    @Test
    void modulesOnlyTalkThroughPublicFacadesOrEvents() {
        slices().matching("studio.quedena.template.(*)..")
                .namingSlices("BC $1")
                .should().notDependOnEachOther()
                .ignoreDependency(
                        resideInAPackage("studio.quedena.template.."),
                        resideInAnyPackage("..interfaces.acl..", "..domain.model.events.."))
                .ignoreDependency(
                        resideInAPackage("studio.quedena.template.."),
                        resideInAPackage("studio.quedena.template.shared.."))
                .check(importedClasses);
    }

    // Regla 4 — Sin ciclos entre Bounded Contexts
    @Test
    void boundedContextsAreFreeOfCycles() {
        slices().matching("studio.quedena.template.(*)..")
                .should().beFreeOfCycles()
                .check(importedClasses);
    }

    // Regla 5 — shared no depende de ningún Bounded Context
    @Test
    void sharedDoesNotDependOnAnyBoundedContext() {
        noClasses().that().resideInAPackage("studio.quedena.template.shared..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "studio.quedena.template.iam.domain..",
                        "studio.quedena.template.iam.application..",
                        "studio.quedena.template.iam.infrastructure..",
                        "studio.quedena.template.profiles.domain..",
                        "studio.quedena.template.profiles.application..",
                        "studio.quedena.template.profiles.infrastructure..")
                .check(importedClasses);
    }

    // Naming — fachadas OHS (si se agregan) viven en interfaces.acl.
    // allowEmptyShould(true): hoy ningún BC necesita fachada OHS (profiles solo
    // consume el evento UserRegistered), la regla queda lista para cuando se agregue una.
    @Test
    void contextFacadesResideInInterfacesAcl() {
        classes().that().haveSimpleNameEndingWith("ContextFacade")
                .should().resideInAPackage("..interfaces.acl..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
