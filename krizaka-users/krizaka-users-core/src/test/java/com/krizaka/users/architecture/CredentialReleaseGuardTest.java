package com.krizaka.users.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.users.domain.ports.inbound.IdentityService;
import com.krizaka.users.domain.ports.outbound.UserCredentialRepositoryPort;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * GHSA-3pf7-rgf2-4x3m: a decrypted provider key leaves krizaka-users only against a run-bound,
 * single-use grant. A service token alone (the shared secret) is never enough.
 */
class CredentialReleaseGuardTest {

  private static final String PKG = "com.krizaka.users";
  private static final String GRANT_SERVICE =
      "com.krizaka.users.application.service.CredentialGrantServiceImpl";
  private static final String IDENTITY_SERVICE =
      "com.krizaka.users.application.service.IdentityServiceImpl";
  private static final String KEY_RESPONSE =
      "com.krizaka.users.infrastructure.adapter.rest.dto.InternalCredentialResponse";
  private static final String GRANT_BODY =
      "com.krizaka.users.infrastructure.adapter.rest.dto.CredentialUseRequest";

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(PKG);
  }

  @Test
  @DisplayName("only the grant service asks for a decrypted key")
  void onlyTheGrantServiceDecrypts() {
    noClasses()
        .that()
        .doNotHaveFullyQualifiedName(GRANT_SERVICE)
        .should()
        .callMethodWhere(
            DescribedPredicate.describe(
                "IdentityService.getDecryptedApiKey",
                call ->
                    call.getTarget().getName().equals("getDecryptedApiKey")
                        && call.getTargetOwner().isAssignableTo(IdentityService.class)))
        .because("a decrypted key is released only against a run-bound grant (GHSA-3pf7-rgf2-4x3m)")
        .check(classes);
  }

  @Test
  @DisplayName("only IdentityServiceImpl opens a stored key")
  void onlyTheIdentityServiceOpensAStoredKey() {
    noClasses()
        .that()
        .doNotHaveFullyQualifiedName(IDENTITY_SERVICE)
        .should()
        .callMethodWhere(
            DescribedPredicate.describe(
                "UserCredentialRepositoryPort.findApiKeyByUserIdAndProviderName",
                call ->
                    call.getTarget().getName().equals("findApiKeyByUserIdAndProviderName")
                        && call.getTargetOwner()
                            .isAssignableTo(UserCredentialRepositoryPort.class)))
        .because("the grant service is the only way to a decrypted key (GHSA-3pf7-rgf2-4x3m)")
        .check(classes);
  }

  @Test
  @DisplayName("every endpoint that answers a key takes a grant in its body")
  void everyKeyAnsweringEndpointTakesAGrant() {
    List<String> offenders =
        classes.stream()
            .filter(c -> c.isAnnotatedWith(RestController.class))
            .flatMap(c -> Arrays.stream(load(c.getName()).getDeclaredMethods()))
            .filter(CredentialReleaseGuardTest::answersAKey)
            .filter(m -> !takesAGrant(m))
            .map(m -> m.getDeclaringClass().getSimpleName() + "#" + m.getName())
            .toList();

    assertThat(offenders).as("endpoints that hand back a decrypted key without a grant").isEmpty();
    assertThat(classes.contain(KEY_RESPONSE)).isTrue();
  }

  private static boolean answersAKey(Method m) {
    return m.getGenericReturnType().getTypeName().contains(KEY_RESPONSE);
  }

  private static boolean takesAGrant(Method m) {
    for (Parameter p : m.getParameters()) {
      if (p.isAnnotationPresent(RequestBody.class) && p.getType().getName().equals(GRANT_BODY)) {
        return true;
      }
    }
    return false;
  }

  private static Class<?> load(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
