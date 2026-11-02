import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, relative } from "node:path";
import test from "node:test";
import { compare, modulesOf, parsePmdBaseline, parseSpotbugsBaseline, pmdViolations, renderPmd, renderSpotbugs, spotbugsViolations } from "./quality-baseline.mjs";

const pmdXml = `<pmd><file name="A.java"><violation rule="CognitiveComplexity" class="A" package="p"/><violation rule="UnusedFormalParameter" class="A" package="p"/><violation rule="CognitiveComplexity" class="A" package="p"/></file></pmd>`;
const sbXml = `<BugCollection><BugInstance type="SQL_INJECTION_JDBC"><Class classname="p.Dao" primary="true"/></BugInstance></BugCollection>`;

test("PMD violations are read per class and rule, once", () => {
  assert.deepEqual([...pmdViolations(pmdXml)].sort((a, b) => a.localeCompare(b)), ["p.A=CognitiveComplexity", "p.A=UnusedFormalParameter"]);
});
test("SpotBugs violations are read per primary class and pattern", () => {
  assert.deepEqual([...spotbugsViolations(sbXml)], ["p.Dao=SQL_INJECTION_JDBC"]);
});
test("a baseline round-trips through its file", () => {
  const pmd = pmdViolations(pmdXml);
  assert.deepEqual(parsePmdBaseline(renderPmd(pmd)), pmd);
  const sb = spotbugsViolations(sbXml);
  assert.deepEqual(parseSpotbugsBaseline(renderSpotbugs(sb)), sb);
});
test("a new violation is refused and a fixed one leaves the baseline", () => {
  const r = compare(new Set(["a=R", "b=R"]), new Set(["b=R", "c=R"]));
  assert.deepEqual(r.added, ["c=R"]);
  assert.deepEqual(r.removed, ["a=R"]);
  assert.deepEqual([...r.next], ["b=R"]);
});
test("SpotBugs reports use single quotes: they are read too", () => {
  const xml = `<BugCollection><BugInstance type='REDOS' priority='2'><Class classname='p.Rules' primary='true'/></BugInstance></BugCollection>`;
  assert.deepEqual([...spotbugsViolations(xml)], ["p.Rules=REDOS"]);
});
test("nested modules (starters/, examples/) are found, build output is not", () => {
  const root = mkdtempSync(join(tmpdir(), "qb-"));
  try {
    for (const m of [".", "core", "starters/web", "examples/app", "target/classes-x"]) {
      mkdirSync(join(root, m, "src/main/java"), { recursive: true });
      writeFileSync(join(root, m, "pom.xml"), "<project/>");
    }
    mkdirSync(join(root, "docs"));
    const found = modulesOf(root).map((d) => relative(root, d) || ".").sort();
    assert.deepEqual(found, [".", "core", "examples/app", "starters/web"]);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});
