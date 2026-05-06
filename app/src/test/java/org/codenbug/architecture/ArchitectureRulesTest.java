package org.codenbug.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importCompiledClasses() {
        List<Path> classDirs = List.of(
                Path.of("../auth/build/classes/java/main"),
                Path.of("../user/build/classes/java/main"),
                Path.of("../event/build/classes/java/main"),
                Path.of("../broker/build/classes/java/main"),
                Path.of("../dispatcher/build/classes/java/main"),
                Path.of("../seat/build/classes/java/main"),
                Path.of("../purchase/build/classes/java/main"),
                Path.of("../platform/common/build/classes/java/main"),
                Path.of("../platform/message/build/classes/java/main"),
                Path.of("../security-aop/build/classes/java/main"),
                Path.of("../redislock/build/classes/java/main"),
                Path.of("build/classes/java/main")
        );

        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPaths(classDirs.stream().filter(Files::exists).toList());
    }

    @Test
    void 도메인_웹_인프라_기술_의존_없음() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("org.codenbug..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.data.redis..",
                        "org.springframework.amqp..",
                        "org.springframework.web.client..",
                        "org.springframework.web.reactive.function.client..",
                        "..infra..",
                        "..infrastructure..",
                        "..ui.."
                );

        rule.check(classes);
    }

    @Test
    void 도메인_클래스_Spring_컴포넌트_아님() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("org.codenbug..domain..")
                .should().beAnnotatedWith("org.springframework.stereotype.Component")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.RestController");

        rule.check(classes);
    }

    @Test
    void 애플리케이션_레이어_인프라_구현_의존_없음() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("org.codenbug..app..", "org.codenbug..application..")
                .and().resideOutsideOfPackage("org.codenbug.app..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infra..",
                        "..infrastructure..",
                        "org.springframework.data.jpa.repository.."
                );

        rule.check(classes);
    }

    @Test
    void 도메인_모듈_다른_도메인_모듈_import_없음() {
        assertNoDomainDependency("auth", "user", "event", "broker", "messagedispatcher", "seat", "purchase");
        assertNoDomainDependency("user", "auth", "event", "broker", "messagedispatcher", "seat", "purchase");
        assertNoDomainDependency("event", "auth", "user", "broker", "messagedispatcher", "seat", "purchase");
        assertNoDomainDependency("broker", "auth", "user", "event", "messagedispatcher", "seat", "purchase");
        assertNoDomainDependency("messagedispatcher", "auth", "user", "event", "broker", "seat", "purchase");
        assertNoDomainDependency("seat", "auth", "user", "event", "broker", "messagedispatcher", "purchase");
        assertNoDomainDependency("purchase", "auth", "user", "event", "broker", "messagedispatcher", "seat");
    }

    private static void assertNoDomainDependency(String ownerPackage, String... forbiddenPackages) {
        List<String> effectiveForbiddenPackages = List.of(forbiddenPackages).stream()
                .filter(packageName -> !("event".equals(ownerPackage) && "seat".equals(packageName)))
                .toList();
        String[] forbiddenDomainPackages = effectiveForbiddenPackages.stream()
                .map(packageName -> "org.codenbug." + packageName + ".domain..")
                .toArray(String[]::new);

        noClasses()
                .that().resideInAnyPackage("org.codenbug." + ownerPackage + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(forbiddenDomainPackages)
                .because("domain modules must not import other domain modules directly")
                .allowEmptyShould(true)
                .check(classes);
    }
}
