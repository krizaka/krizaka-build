// ci.yml: a republish can name the repository whose package URL receives the modules (a package is bound to its first repository, HTTP 422 otherwise).
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const ci = readFileSync(new URL("../.github/workflows/ci.yml", import.meta.url), "utf8");

test("the dispatch has a deploy-repository input", () => {
  assert.match(ci, /\n {6}deploy-repository:\n(?: {8}.*\n)*? {8}type: string/);
});

test("it is passed to the org pipeline next to deploy-modules", () => {
  assert.match(ci, /deploy-modules: \$\{\{ inputs\.deploy-modules \|\| '' \}\}\n {6}deploy-repository: \$\{\{ inputs\.deploy-repository \|\| '' \}\}/);
});
