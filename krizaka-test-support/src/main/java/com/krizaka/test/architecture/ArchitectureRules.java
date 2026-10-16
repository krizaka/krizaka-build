package com.krizaka.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The structural rules every Krizaka JVM service holds: layers, cycles, names, where configuration
 * is read and where a transaction starts.
 *
 * <p>Each rule exists twice. {@code xxxRule(...)} returns the {@link ArchRule}, so a service can
 * <em>freeze</em> it with {@link FrozenRules#check} (the violations that exist today are recorded
 * and may only shrink); {@code assertXxx(...)} checks it strictly, for a service with no debt on
 * it. Annotations are matched by fully qualified name, so this library needs no Spring web or
 * transaction jar on its own classpath.
 *
 * <p>No rule here lets itself pass over nothing (GOV-006): a module applies the rules whose subject
 * it has — a library without controllers does not apply {@link #controllersNamedControllerRule()}.
 *
 * <pre>{@code
 * @Test
 * void layers() {
 *   FrozenRules.check(
 *       ArchitectureRules.layersRule("..web..", "..service..", "..repository.."), CLASSES);
 * }
 * }</pre>
 */
public final class ArchitectureRules {

  private static final String REST_CONTROLLER =
      "org.springframework.web.bind.annotation.RestController";
  private static final String CONTROLLER = "org.springframework.stereotype.Controller";
  private static final String SERVICE = "org.springframework.stereotype.Service";
  private static final String REPOSITORY = "org.springframework.stereotype.Repository";
  private static final String SPRING_DATA_REPOSITORY =
      "org.springframework.data.repository.Repository";
  private static final String VALUE = "org.springframework.beans.factory.annotation.Value";
  private static final String TRANSACTIONAL =
      "org.springframework.transaction.annotation.Transactional";
  private static final String JAKARTA_TRANSACTIONAL = "jakarta.transaction.Transactional";
  private static final String CONFIGURATION =
      "org.springframework.context.annotation.Configuration";
  private static final String AUTO_CONFIGURATION =
      "org.springframework.boot.autoconfigure.AutoConfiguration";
  private static final String SPRING_ENVIRONMENT = "org.springframework.core.env.Environment";

  private ArchitectureRules() {}

  /**
   * Web calls service, service calls repository, nothing skips a layer and nothing calls up.
   *
   * @param webPackage the web layer's package pattern, e.g. {@code "..web.."}
   * @param servicePackage the service layer's package pattern
   * @param repositoryPackage the repository layer's package pattern
   * @return the rule
   */
  public static ArchRule layersRule(
      String webPackage, String servicePackage, String repositoryPackage) {
    return layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Web")
        .definedBy(webPackage)
        .layer("Service")
        .definedBy(servicePackage)
        .layer("Repository")
        .definedBy(repositoryPackage)
        .whereLayer("Web")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("Service")
        .mayOnlyBeAccessedByLayers("Web")
        .whereLayer("Repository")
        .mayOnlyBeAccessedByLayers("Service")
        .because("web calls service, service calls repository: no layer is skipped, none calls up");
  }

  /**
   * {@link #layersRule} checked strictly.
   *
   * @param classes the imported production classes
   * @param webPackage the web layer's package pattern
   * @param servicePackage the service layer's package pattern
   * @param repositoryPackage the repository layer's package pattern
   */
  public static void assertLayers(
      JavaClasses classes, String webPackage, String servicePackage, String repositoryPackage) {
    layersRule(webPackage, servicePackage, repositoryPackage).check(classes);
  }

  /**
   * No cycle between the direct sub-packages of the module.
   *
   * @param modulePackage the module's root package, e.g. {@code "com.krizaka.users"}
   * @return the rule
   */
  public static ArchRule noPackageCyclesRule(String modulePackage) {
    return slices()
        .matching(modulePackage + ".(*)..")
        .should()
        .beFreeOfCycles()
        .because("a cycle between packages cannot be built, tested or understood one at a time");
  }

  /**
   * {@link #noPackageCyclesRule} checked strictly.
   *
   * @param classes the imported production classes
   * @param modulePackage the module's root package
   */
  public static void assertNoPackageCycles(JavaClasses classes, String modulePackage) {
    noPackageCyclesRule(modulePackage).check(classes);
  }

  /**
   * A web endpoint is named {@code *Controller}.
   *
   * @return the rule
   */
  public static ArchRule controllersNamedControllerRule() {
    return classes()
        .that()
        .areAnnotatedWith(REST_CONTROLLER)
        .or()
        .areAnnotatedWith(CONTROLLER)
        .should()
        .haveSimpleNameEndingWith("Controller")
        .because("a class that serves HTTP says so in its name");
  }

  /**
   * A Spring {@code @Service} is named {@code *Service} (or {@code *ServiceImpl}).
   *
   * @return the rule
   */
  public static ArchRule servicesNamedServiceRule() {
    return classes()
        .that()
        .areAnnotatedWith(SERVICE)
        .should()
        .haveSimpleNameEndingWith("Service")
        .orShould()
        .haveSimpleNameEndingWith("ServiceImpl")
        .because("services are named after the capability they serve, not the pattern they use");
  }

  /**
   * A repository (Spring Data, or {@code @Repository}) is named {@code *Repository}.
   *
   * @return the rule
   */
  public static ArchRule repositoriesNamedRepositoryRule() {
    return classes()
        .that()
        .areAnnotatedWith(REPOSITORY)
        .or()
        .areAssignableTo(SPRING_DATA_REPOSITORY)
        .should()
        .haveSimpleNameEndingWith("Repository")
        .because("the one place that reads and writes the database is recognisable by its name");
  }

  /**
   * The three naming rules checked strictly.
   *
   * @param classes the imported production classes
   */
  public static void assertNamingConventions(JavaClasses classes) {
    controllersNamedControllerRule().check(classes);
    servicesNamedServiceRule().check(classes);
    repositoriesNamedRepositoryRule().check(classes);
  }

  /**
   * {@code @Value} only in a configuration class.
   *
   * <p>A parameter is read from the settings (database) or bound once at the edge by a
   * {@code @ConfigurationProperties} class; business code receives it as a typed parameter.
   *
   * @param configPackage the configuration package pattern, e.g. {@code "..config.."}
   * @return the rule
   */
  public static ArchRule valueOnlyInConfigRule(String configPackage) {
    return fields()
        .that()
        .areAnnotatedWith(VALUE)
        .should()
        .beDeclaredInClassesThat()
        .resideInAPackage(configPackage)
        .orShould()
        .beDeclaredInClassesThat()
        .areAnnotatedWith(CONFIGURATION)
        .orShould()
        .beDeclaredInClassesThat()
        .areAnnotatedWith(AUTO_CONFIGURATION)
        .orShould()
        .beDeclaredInClassesThat()
        .areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
        .because(
            "a parameter is bound once at the edge or read from the settings, never scattered");
  }

  /**
   * The environment (the process variables, Spring's {@code Environment}) is read in configuration
   * only.
   *
   * @param configPackage the configuration package pattern, e.g. {@code "..config.."}
   * @return the rule
   */
  public static ArchRule noEnvironmentOutsideConfigRule(String configPackage) {
    return noClasses()
        .that()
        .resideOutsideOfPackage(configPackage)
        .and()
        .areNotAnnotatedWith(CONFIGURATION)
        .and()
        .areNotAnnotatedWith(AUTO_CONFIGURATION)
        .should()
        .callMethod(System.class, "getenv")
        .orShould()
        .callMethod(System.class, "getenv", String.class)
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName(SPRING_ENVIRONMENT)
        .because(
            "domain and service code receive their parameters, they do not read the environment");
  }

  /**
   * Both configuration rules checked strictly.
   *
   * @param classes the imported production classes
   * @param configPackage the configuration package pattern
   */
  public static void assertConfigurationReadOnlyInConfig(
      JavaClasses classes, String configPackage) {
    valueOnlyInConfigRule(configPackage).check(classes);
    noEnvironmentOutsideConfigRule(configPackage).check(classes);
  }

  /**
   * A transaction starts on a service, never on a controller, a repository adapter or the domain.
   *
   * @param servicePackages the package patterns where {@code @Transactional} is allowed
   * @return the rule
   */
  public static ArchRule transactionalOnlyOnServicesRule(String... servicePackages) {
    return methods()
        .that()
        .areAnnotatedWith(TRANSACTIONAL)
        .or()
        .areAnnotatedWith(JAKARTA_TRANSACTIONAL)
        .should()
        .beDeclaredInClassesThat()
        .resideInAnyPackage(servicePackages)
        .because("the transactional boundary is the use case, which is a service");
  }

  /**
   * {@link #transactionalOnlyOnServicesRule} checked strictly.
   *
   * @param classes the imported production classes
   * @param servicePackages the package patterns where {@code @Transactional} is allowed
   */
  public static void assertTransactionalOnlyOnServices(
      JavaClasses classes, String... servicePackages) {
    transactionalOnlyOnServicesRule(servicePackages).check(classes);
  }
}
