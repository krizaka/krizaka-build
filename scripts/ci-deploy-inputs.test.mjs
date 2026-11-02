// ci.yml: a republish can name the repository whose package URL receives the modules (a package is bound to its first repository, HTTP 422 otherwise).
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const ci = readFileSync(new URL("../.github/workflows/ci.yml", import.meta.url), "utf8");

test("the dispatch has a deploy-repository input", () => {
  assert.match(ci, /\n {6}deploy-repository:\n(?: {8}.*\n)*? {8}type: string/);
});

test("it is passed to the org pipeline next to deploy-modules", () => {
  assert.match(ci, /deploy-modules: \$\{\{ needs\.deploy-plan\.outputs\.modules \}\}\n {6}deploy-repository: \$\{\{ inputs\.deploy-repository \|\| '' \}\}/);
});

test("the tag's own deploy leaves out the modules of the other repositories, so billing is never behind a 422", async () => {
  const { ownDeployModules } = await import("./release-follow-ups.mjs");
  const list = ownDeployModules(new URL("..", import.meta.url).pathname).split(",");
  assert.ok(list.includes("!krizaka-platform-kit") && list.includes("!krizaka-users/krizaka-users-client"));
  assert.ok(!list.some((m) => m.includes("billing")));
  assert.match(ci, /REQUESTED:-\$\(node scripts\/release-follow-ups\.mjs own-modules\)/);
});
