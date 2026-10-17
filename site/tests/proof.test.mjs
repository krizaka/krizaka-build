import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { countTests, orazakaServices, platformOf } from "../scripts/gen-proof.mjs";

function tree(files) {
  const dir = mkdtempSync(join(tmpdir(), "kz-java-proof-"));
  for (const [name, content] of Object.entries(files)) {
    mkdirSync(dirname(join(dir, name)), { recursive: true });
    writeFileSync(join(dir, name), content);
  }
  return dir;
}

test("the tests are counted, and the ones that start a real PostgreSQL or RabbitMQ apart", () => {
  const dir = tree({
    "a/src/test/java/PlainTest.java": "class PlainTest { @Test void a() {} @Test void b() {} @ParameterizedTest void c() {} }",
    "a/src/test/java/OutboxIT.java": "class OutboxIT extends AbstractContainerIntegrationTest { @Test void x() {} @Test void y() {} }",
    "a/src/test/java/BrokerIT.java": "@Testcontainers class BrokerIT { RabbitMQContainer r; @Test void z() {} }",
    "a/src/main/java/Main.java": "class Main { @Test void notATest() {} }",
    "a/.cache/repos/b/src/test/java/CloneTest.java": "@Test",
  });
  assert.deepEqual(countTests(dir), { tests: 6, realInfrastructure: 3 });
});

test("a main source is never a test, and an empty tree has none", () => {
  assert.deepEqual(countTests(tree({ "a/src/main/java/M.java": "@Test" })), { tests: 0, realInfrastructure: 0 });
});

test("Orazaka's services are the deployable modules that depend on a Krizaka artifact", () => {
  const dep = (a) => `<dependency><groupId>com.krizaka</groupId><artifactId>${a}</artifactId></dependency>`;
  const pom = (id, deps = "", packaging = "") => `<project><artifactId>${id}</artifactId>${packaging}<dependencies>${deps}</dependencies></project>`;
  const dir = tree({
    "orazaka-job-service/pom.xml": pom("orazaka-job-service", dep("krizaka-billing-client") + dep("krizaka-security")),
    "orazaka-edge/pom.xml": pom("orazaka-edge", dep("krizaka-web")),
    "orazaka-studio/orazaka-studio-client/pom.xml": pom("orazaka-studio-client", dep("krizaka-security")),
    "orazaka-studio/orazaka-studio-api/pom.xml": pom("orazaka-studio-api"),
    "orazaka-studio/pom.xml": pom("orazaka-studio", dep("krizaka-bom"), "<packaging>pom</packaging>"),
    "orazaka-lonely-service/pom.xml": pom("orazaka-lonely-service"),
  });
  assert.deepEqual(orazakaServices([dir]), [
    { id: "orazaka-edge", artifacts: ["krizaka-web"] },
    { id: "orazaka-job-service", artifacts: ["krizaka-billing-client", "krizaka-security"] },
  ]);
});

test("the platform is read from the parent POM, never typed", () => {
  const pom = "<properties><java.version>21</java.version><spring-boot.version>4.0.8</spring-boot.version></properties>";
  assert.deepEqual(platformOf(pom), { java: "21", springBoot: "4.0.8", springBootMinor: "4.0" });
  assert.throws(() => platformOf("<properties/>"), /java\.version/);
});
