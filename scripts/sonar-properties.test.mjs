// The billing scripts are PostgreSQL: the Oracle PL/SQL rule plsql:CharVarchar must not fail the Sonar gate on them (the import PR went red on CHAR(3)).
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const properties = readFileSync(new URL("../sonar-project.properties", import.meta.url), "utf8");

test("plsql:CharVarchar is ignored on SQL files", () => {
  assert.match(properties, /ignore\.multiCriteria\.postgresSql\.ruleKey=plsql:CharVarchar/);
  assert.match(properties, /ignore\.multiCriteria\.postgresSql\.resourceKey=\*\*\/\*\.sql/);
});

// The Maven scanner ignores sonar-project.properties: the same ignore must be in the root pom.
test("plsql:CharVarchar is ignored on SQL files in the root pom (Maven scanner)", () => {
  const pom = readFileSync(new URL("../pom.xml", import.meta.url), "utf8");
  assert.match(pom, /<sonar\.issue\.ignore\.multicriteria\.postgresSql\.ruleKey>plsql:CharVarchar</);
  assert.match(pom, /<sonar\.issue\.ignore\.multicriteria\.postgresSql\.resourceKey>\*\*\/\*\.sql</);
});
