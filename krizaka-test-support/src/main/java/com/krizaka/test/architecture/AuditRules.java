package com.krizaka.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * The build rules of ADR-015 (krizaka/.github#384, decision 4): the structural lessons of the
 * AI-code audit, as ArchUnit rules a governance test freezes with {@link FrozenRules#check}. The
 * debt of a repository is recorded once ({@code archunit_store/}), new code is held to the rule,
 * the debt only shrinks.
 *
 * <pre>{@code
 * @Test
 * void controllersDoNotReachPersistence() {
 *   FrozenRules.check(AuditRules.controllersDoNotReachPersistenceRule(), CLASSES);
 * }
 * }</pre>
 *
 * <p>There is deliberately no {@code *Manager} rule (ADR-015 §5): a name says nothing about a
 * design.
 */
public final class AuditRules {

  private static final String REST_CONTROLLER =
      "org.springframework.web.bind.annotation.RestController";
  private static final String CONTROLLER = "org.springframework.stereotype.Controller";
  private static final String COMPONENT = "org.springframework.stereotype.Component";
  private static final String REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";
  private static final String VALID = "jakarta.validation.Valid";
  private static final String VALIDATED = "org.springframework.validation.annotation.Validated";
  private static final String ENTITY = "jakarta.persistence.Entity";
  private static final String SPRING_DATA_REPOSITORY =
      "org.springframework.data.repository.Repository";
  private static final String NULL_MARKED = "org.jspecify.annotations.NullMarked";

  private AuditRules() {}

  /**
   * Controllers do not reach persistence: no dependency on a type of a {@code ..persistence..}
   * package, a JPA entity or a Spring Data repository (the service in between owns the transaction
   * and the mapping).
   *
   * @return the rule
   */
  public static ArchRule controllersDoNotReachPersistenceRule() {
    return noClasses()
        .that()
        .areAnnotatedWith(REST_CONTROLLER)
        .or()
        .areAnnotatedWith(CONTROLLER)
        .should()
        .dependOnClassesThat(persistenceTypes())
        .as("no controller depends on a persistence type")
        .because(
            "a controller speaks HTTP; the service between it and the store owns the transaction, "
                + "the mapping and the rules (ADR-015)");
  }

  /**
   * No class is named {@code *Utils}, {@code *Util}, {@code *Helper} or {@code *Helpers}: the
   * behaviour belongs to a type that owns the data it works on.
   *
   * @return the rule
   */
  public static ArchRule noUtilityClassesRule() {
    return noClasses()
        .should()
        .haveSimpleNameEndingWith("Utils")
        .orShould()
        .haveSimpleNameEndingWith("Util")
        .orShould()
        .haveSimpleNameEndingWith("Helper")
        .orShould()
        .haveSimpleNameEndingWith("Helpers")
        .as("no *Utils / *Helper class")
        .because(
            "a bag of static helpers has no owner: put the behaviour on the type it works on (ADR-015)");
  }

  /**
   * A {@code @RequestBody} DTO is validated: its parameter also carries {@code @Valid} (or
   * {@code @Validated}). Parameters of JDK types ({@code String}, {@code Map}, {@code byte[]}) are
   * exempt.
   *
   * @return the rule
   */
  public static ArchRule requestBodyIsValidatedRule() {
    return methods()
        .should(haveValidatedRequestBodies())
        .as("every @RequestBody DTO is @Valid")
        .because(
            "the constraints of a request record are only checked at the boundary when @Valid asks (ADR-015)");
  }

  /**
   * Instance fields of Spring beans ({@code @Component} and its stereotypes) are final: a bean is
   * built once, with its collaborators, and holds no state that changes.
   *
   * @return the rule
   */
  public static ArchRule springBeanFieldsAreFinalRule() {
    return fields()
        .that()
        .areNotStatic()
        .and()
        .areDeclaredInClassesThat()
        .areMetaAnnotatedWith(COMPONENT)
        .and()
        .areDeclaredInClassesThat()
        .areNotRecords()
        .should()
        .haveModifier(JavaModifier.FINAL)
        .as("instance fields of Spring beans are final")
        .because(
            "a bean is shared by every request: a field that changes is shared mutable state (ADR-015)");
  }

  /**
   * Every package is {@code @NullMarked} (JSpecify): its {@code package-info} carries the
   * annotation, so NullAway checks it. Frozen, a repository marks its packages one by one, never in
   * one flag day.
   *
   * @return the rule
   */
  public static ArchRule everyPackageNullMarkedRule() {
    return classes()
        .that()
        .doNotHaveSimpleName("package-info")
        .should(resideInANullMarkedPackage())
        .as("every package is @NullMarked")
        .because(
            "NullAway only checks what is marked; null is part of a contract, not a convention (ADR-015)");
  }

  /**
   * Asserts {@link #controllersDoNotReachPersistenceRule()}.
   *
   * @param classes the imported production classes
   */
  public static void assertControllersDoNotReachPersistence(JavaClasses classes) {
    controllersDoNotReachPersistenceRule().check(classes);
  }

  /**
   * Asserts {@link #noUtilityClassesRule()}.
   *
   * @param classes the imported production classes
   */
  public static void assertNoUtilityClasses(JavaClasses classes) {
    noUtilityClassesRule().check(classes);
  }

  /**
   * Asserts {@link #requestBodyIsValidatedRule()}.
   *
   * @param classes the imported production classes
   */
  public static void assertRequestBodyIsValidated(JavaClasses classes) {
    requestBodyIsValidatedRule().check(classes);
  }

  /**
   * Asserts {@link #springBeanFieldsAreFinalRule()}.
   *
   * @param classes the imported production classes
   */
  public static void assertSpringBeanFieldsAreFinal(JavaClasses classes) {
    springBeanFieldsAreFinalRule().check(classes);
  }

  /**
   * Asserts {@link #everyPackageNullMarkedRule()}.
   *
   * @param classes the imported production classes
   */
  public static void assertEveryPackageNullMarked(JavaClasses classes) {
    everyPackageNullMarkedRule().check(classes);
  }

  private static DescribedPredicate<JavaClass> persistenceTypes() {
    return DescribedPredicate.describe(
        "persistence types (a ..persistence.. package, a JPA entity, a Spring Data repository)",
        type ->
            type.getPackageName().contains(".persistence")
                || type.isAnnotatedWith(ENTITY)
                || type.isAssignableTo(SPRING_DATA_REPOSITORY));
  }

  private static ArchCondition<JavaMethod> haveValidatedRequestBodies() {
    return new ArchCondition<>("validate every @RequestBody DTO with @Valid") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        for (JavaParameter parameter : method.getParameters()) {
          if (parameter.isAnnotatedWith(REQUEST_BODY)
              && isDto(parameter.getRawType())
              && !parameter.isAnnotatedWith(VALID)
              && !parameter.isAnnotatedWith(VALIDATED)) {
            events.add(
                SimpleConditionEvent.violated(
                    method,
                    String.format(
                        "%s takes @RequestBody %s without @Valid",
                        method.getFullName(), parameter.getRawType().getSimpleName())));
          }
        }
      }
    };
  }

  private static boolean isDto(JavaClass type) {
    return !type.isPrimitive() && !type.isArray() && !type.getPackageName().startsWith("java.");
  }

  private static ArchCondition<JavaClass> resideInANullMarkedPackage() {
    return new ArchCondition<>("reside in a package annotated @NullMarked") {
      private final Set<String> reported = new HashSet<>();

      @Override
      public void init(java.util.Collection<JavaClass> allObjectsToTest) {
        reported.clear();
      }

      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        JavaClass outer = outermost(javaClass);
        String packageName = javaClass.getPackageName();
        boolean marked =
            javaClass.getPackage().isAnnotatedWith(NULL_MARKED)
                || outer.isAnnotatedWith(NULL_MARKED);
        if (!marked && reported.add(packageName)) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass,
                  String.format(
                      "Package <%s> is not @NullMarked (no package-info with it)", packageName)));
        }
      }
    };
  }

  private static JavaClass outermost(JavaClass javaClass) {
    JavaClass current = javaClass;
    while (current.getEnclosingClass().isPresent()) {
      current = current.getEnclosingClass().get();
    }
    return current;
  }
}
