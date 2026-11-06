// krizaka-internal#159: every module with sources is verified under the consumers' PMD/SpotBugs rules in CI.
import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const read = (p) => readFileSync(join(root, p), "utf8");

// krizaka-quality holds the rules themselves (it skips PMD, its own tests run them on samples).
const RULES_MODULE = "krizaka-quality";

const modules = [...read("pom.xml").matchAll(/<module>([^<]+)<\/module>/g)].map((m) => m[1]);
const ci = read(".github/workflows/ci.yml");
const line = ci.split("\n").find((l) => l.includes("# pmd-modules"));

test("the reactor lists modules", () => assert.ok(modules.length > 0));

test("the PMD job verifies with the checks on", () => {
  assert.ok(line, "a step marked # pmd-modules exists in ci.yml");
  assert.match(line, /-Dkrizaka\.quality\.phase=verify/);
  assert.doesNotMatch(line, /-Dkrizaka\.quality\.phase=none/);
});

test("every module with sources is in the PMD job", () => {
  const listed = (line.match(/-pl\s+(\S+)/)?.[1] ?? "").split(",");
  for (const m of modules) {
    if (m === RULES_MODULE) continue;
    if (!existsSync(join(root, m, "src/main"))) continue;
    assert.ok(listed.includes(m), `${m} has sources but is not verified under the consumers' rules (ci.yml pmd-modules)`);
  }
});

test("the ruleset is the single krizaka-quality file the consumers inherit", () => {
  const parent = read("krizaka-parent/pom.xml");
  assert.match(parent, /<ruleset>krizaka-quality\/pmd-ruleset\.xml<\/ruleset>/);
  assert.ok(existsSync(join(root, RULES_MODULE, "src/main/resources/krizaka-quality/pmd-ruleset.xml")));
});
