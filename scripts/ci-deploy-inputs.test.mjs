// ci.yml: a republish on a tag can name its modules and the repository whose package URL receives them.
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

test("the tag's own deploy is the whole reactor: no module is left to a deleted repository (krizaka-internal#2656)", () => {
  assert.match(ci, /run: echo "modules=\$\{REQUESTED\}" >> "\$GITHUB_OUTPUT"/);
  assert.doesNotMatch(ci, /release-follow-ups/);
});

test("the release waits for the tag's deploy, then checks every module on the registry", () => {
  const release = readFileSync(new URL("../.github/workflows/release-please.yml", import.meta.url), "utf8");
  assert.doesNotMatch(release, /release-follow-ups/);
  assert.match(release, /gh run watch "\$id"[^\n]*--exit-status/);
  assert.ok(release.indexOf("gh run watch") < release.indexOf("node scripts/check-published.mjs \"$TAG\""));
  assert.match(release, /follow-ups:[\s\S]*?needs: release-please[\s\S]*?actions: read[\s\S]*?packages: read/);
});
