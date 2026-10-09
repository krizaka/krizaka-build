package com.krizaka.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Code rules every Krizaka repository holds, as ArchUnit checks a governance test calls.
 *
 * <pre>{@code
 * class UsersGovernanceTest {
 *   private static final JavaClasses CLASSES =
 *       new ClassFileImporter()
 *           .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
 *           .importPackages("com.krizaka.users");
 *
 *   @Test
 *   void oneClassPerFile() {
 *     CodeRules.assertOneTopLevelClassPerFile(CLASSES, "com.krizaka.users");
 *   }
 * }
 * }</pre>
 *
 * <p>Each rule names the reason it exists in its failure message, so a violation explains itself.
 */
public final class CodeRules {

  private static final String MAPPER_SUFFIX = "Mapper";
  private static final String JPA_ENTITY = "jakarta.persistence.Entity";

  private CodeRules() {}

  /**
   * The production classes of a package, tests excluded — the input every rule below takes.
   *
   * @param basePackage the root package to import
   * @return the imported classes
   */
  public static JavaClasses importProductionClasses(String basePackage) {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(basePackage);
  }

  /**
   * Every top-level class lives in a file named after it.
   *
   * <p>A second top-level class in a file is invisible to anyone looking for it by name, and is
   * where helpers that should have been a type of their own accumulate.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   */
  public static void assertOneTopLevelClassPerFile(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .and()
        .doNotHaveModifier(JavaModifier.SYNTHETIC)
        .should(
            new ArchCondition<JavaClass>("reside in a dedicated file matching their class name") {
              @Override
              public void check(JavaClass javaClass, ConditionEvents events) {
                if (javaClass.isAnonymousClass() || javaClass.isMemberClass()) {
                  return;
                }
                String sourceFileName =
                    javaClass.getSource().flatMap(Source::getFileName).orElse(null);
                if (sourceFileName != null && !sourceFileName.equals("Unknown Source")) {
                  String expectedFileName = javaClass.getSimpleName() + ".java";
                  if (!sourceFileName.equals(expectedFileName)) {
                    events.add(
                        SimpleConditionEvent.violated(
                            javaClass,
                            String.format(
                                "Class '%s' is bundled inside '%s'.",
                                javaClass.getFullName(), sourceFileName)));
                  }
                }
              }
            })
        .because("every top-level class lives in its own file, named after it")
        .check(classes);
  }

  /**
   * No class writes to {@code System.out} or {@code System.err}.
   *
   * @param classes the imported production classes
   */
  public static void assertNoStandardStreams(JavaClasses classes) {
    GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("output goes through an SLF4J logger, which levels, routes and redacts it")
        .check(classes);
  }

  /**
   * No field is injected with {@code @Autowired}.
   *
   * <p>Constructor injection makes a dependency visible, mandatory and final, and lets a unit test
   * build the class without a container.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   */
  public static void assertNoFieldInjection(JavaClasses classes, String modulePackage) {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage(modulePackage + "..")
        .should()
        .beAnnotatedWith(Autowired.class)
        .because("dependencies are injected through the constructor")
        .check(classes);
  }

  /**
   * Every top-level class of an application-service package is a {@code *Service}.
   *
   * <p>Services are named after the capability they serve; mappers, properties and exceptions live
   * with the code they belong to, and pattern names ({@code *Manager}, {@code *Handler}) say how,
   * not what.
   *
   * @param classes the imported production classes
   * @param servicePackage the application-service package
   */
  public static void assertServicePackageOnlyServices(JavaClasses classes, String servicePackage) {
    classes()
        .that()
        .resideInAPackage(servicePackage + "..")
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Service")
        .because("an application-service package holds capability-named *Service classes only")
        .check(classes);
  }

  /**
   * The domain holds no transport DTOs ({@code *Request}, {@code *Response}).
   *
   * @param classes the imported production classes
   * @param domainPackage the domain package
   */
  public static void assertDomainHasNoTransportDtos(JavaClasses classes, String domainPackage) {
    noClasses()
        .that()
        .resideInAPackage(domainPackage + "..")
        .should()
        .haveSimpleNameEndingWith("Request")
        .orShould()
        .haveSimpleNameEndingWith("Response")
        .because("transport DTOs belong to the adapters that speak the transport, never the domain")
        .check(classes);
  }

  /**
   * No class name repeats the product or organisation name ({@code Engine}, not {@code
   * KrizakaEngine}): the package already says whose it is.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   * @param prefix the forbidden prefix, e.g. {@code "Krizaka"}
   */
  public static void assertNoProductPrefix(
      JavaClasses classes, String modulePackage, String prefix) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .should()
        .haveSimpleNameNotStartingWith(prefix)
        .because("the package already names the product; " + prefix + "* class names repeat it")
        .check(classes);
  }

  /**
   * No anonymous class in production code, except enum constant bodies and Jackson-style {@code
   * TypeReference} captures. Compiler-generated synthetic classes are ignored.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   */
  public static void assertNoAnonymousClasses(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .and()
        .doNotHaveModifier(JavaModifier.SYNTHETIC)
        .should()
        .notBeAnonymousClasses()
        .orShould(beEnumConstantOrTypeReference())
        .because("an anonymous class is a type nobody can name, test or find")
        .check(classes);
  }

  /**
   * Instance fields of concrete, non-record classes are private.
   *
   * @param classes the imported production classes
   */
  public static void assertFieldsPrivate(JavaClasses classes) {
    fields()
        .that()
        .areDeclaredInClassesThat()
        .areNotRecords()
        .and()
        .areDeclaredInClassesThat()
        .areNotEnums()
        .and()
        .areDeclaredInClassesThat()
        .areNotInterfaces()
        .and()
        .areDeclaredInClassesThat()
        .haveSimpleNameNotContaining("Abstract")
        .and()
        .areNotStatic()
        .should(
            new ArchCondition<JavaField>("be private") {
              @Override
              public void check(JavaField field, ConditionEvents events) {
                if (!field.getModifiers().contains(JavaModifier.PRIVATE)) {
                  events.add(
                      SimpleConditionEvent.violated(
                          field,
                          String.format(
                              "Field <%s> in <%s> must be private",
                              field.getName(), field.getOwner().getName())));
                }
              }
            })
        .because("state is reached through behaviour, never through a field")
        .check(classes);
  }

  /**
   * Collection fields ({@code List}, {@code Map}, {@code Set}) of non-record classes are {@code
   * private final}. JPA entities are exempt: the ORM replaces their collections on load.
   *
   * @param classes the imported production classes
   */
  public static void assertCollectionFieldsPrivateFinal(JavaClasses classes) {
    fields()
        .that()
        .haveRawType(List.class)
        .or()
        .haveRawType(Map.class)
        .or()
        .haveRawType(Set.class)
        .and()
        .areDeclaredInClassesThat()
        .areNotRecords()
        .and()
        .areDeclaredInClassesThat()
        .areNotEnums()
        .and()
        .areDeclaredInClassesThat()
        .areNotInterfaces()
        .and()
        .areDeclaredInClassesThat()
        .haveSimpleNameNotContaining("Abstract")
        .and()
        .areDeclaredInClassesThat()
        .areNotAnnotatedWith(JPA_ENTITY)
        .should(bePrivateAndFinal())
        .because("a collection field that can be reassigned or shared is mutable state in disguise")
        .check(classes);
  }

  /**
   * JPA converters, entities and Spring Data repositories live in their own sub-packages of {@code
   * infrastructure.adapter.persistence}.
   *
   * @param classes the imported production classes
   */
  public static void assertPersistencePackageHygiene(JavaClasses classes) {
    classes()
        .that()
        .implement("jakarta.persistence.AttributeConverter")
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.converter..")
        .because("JPA AttributeConverters live in the .converter package")
        .check(classes);
    classes()
        .that()
        .areAnnotatedWith(JPA_ENTITY)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.entity..")
        .because("JPA entities live in the .entity package")
        .check(classes);
    classes()
        .that()
        .areAssignableTo("org.springframework.data.repository.Repository")
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.repository..")
        .because("Spring Data repositories live in the .repository package")
        .check(classes);
  }

  /**
   * The {@code infrastructure.adapter.persistence} package itself (not its sub-packages) holds only
   * {@code *Adapter} and {@code *Mapper} classes: one package, one kind of component.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   */
  public static void assertPersistenceAdapterPackageKind(
      JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + ".infrastructure.adapter.persistence")
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Adapter")
        .orShould()
        .haveSimpleNameEndingWith(MAPPER_SUFFIX)
        .because("adapter/persistence holds outbound-port adapters and their mappers only")
        .check(classes);
  }

  /**
   * A module declares no web controller — for libraries and modules that must not serve HTTP.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   */
  public static void assertNoWebControllers(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .should()
        .notBeAnnotatedWith("org.springframework.web.bind.annotation.RestController")
        .andShould()
        .notBeAnnotatedWith("org.springframework.stereotype.Controller")
        .because("this module serves no HTTP; controllers belong to the service that does")
        .check(classes);
  }

  /**
   * {@code *Mapper} classes are final.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   */
  public static void assertMappersFinal(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .and()
        .haveSimpleNameEndingWith(MAPPER_SUFFIX)
        .should()
        .haveModifier(JavaModifier.FINAL)
        .because("a mapper is a final, stateless set of static functions")
        .check(classes);
  }

  /**
   * {@code *Mapper} classes are not public: they are a detail of the adapter they serve.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package to check
   */
  public static void assertMappersPackagePrivate(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .and()
        .haveSimpleNameEndingWith(MAPPER_SUFFIX)
        .should()
        .notBePublic()
        .because("a mapper is an internal detail of the code it maps for")
        .check(classes);
  }

  /**
   * {@code *Impl} classes are not public: other modules reach them through their interface.
   *
   * @param classes the imported production classes
   * @param servicePackage the package holding the implementations
   */
  public static void assertImplClassesPackagePrivate(JavaClasses classes, String servicePackage) {
    classes()
        .that()
        .resideInAPackage(servicePackage + "..")
        .and()
        .haveSimpleNameEndingWith("Impl")
        .should()
        .notBePublic()
        .because("an implementation is reached through its interface, never by name")
        .check(classes);
  }

  /**
   * The domain layer depends on no framework: no Spring, no JPA, no Jackson.
   *
   * @param classes the imported production classes
   * @param modulePackage the root package whose {@code .domain} is checked
   */
  public static void assertDomainPurity(JavaClasses classes, String modulePackage) {
    noClasses()
        .that()
        .resideInAPackage(modulePackage + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "com.fasterxml.jackson..",
            "tools.jackson..")
        .because("the domain is plain records and classes; frameworks live in the adapters")
        .check(classes);
  }

  private static ArchCondition<JavaClass> beEnumConstantOrTypeReference() {
    return new ArchCondition<>("be an enum constant body or TypeReference") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        boolean isEnumBody = javaClass.getEnclosingClass().map(JavaClass::isEnum).orElse(false);
        boolean isTypeRef =
            javaClass
                .getSuperclass()
                .map(superClass -> superClass.getName().contains("TypeReference"))
                .orElse(false);
        if (!isEnumBody && !isTypeRef) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, "Anonymous class <" + javaClass.getName() + "> is not exempt"));
        }
      }
    };
  }

  private static ArchCondition<JavaField> bePrivateAndFinal() {
    return new ArchCondition<>("be private and final") {
      @Override
      public void check(JavaField field, ConditionEvents events) {
        boolean isPrivate = field.getModifiers().contains(JavaModifier.PRIVATE);
        boolean isFinal = field.getModifiers().contains(JavaModifier.FINAL);
        if (!isPrivate || !isFinal) {
          events.add(
              SimpleConditionEvent.violated(
                  field,
                  String.format(
                      "Field <%s> in <%s> is not private final (private=%s, final=%s)",
                      field.getName(), field.getOwner().getName(), isPrivate, isFinal)));
        }
      }
    };
  }
}
