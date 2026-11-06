import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

// 2026-11-06: krizaka-users, krizaka-platform-kit, krizaka-notifications and krizaka-accounts are deleted (krizaka-billing is gone too):
// their code is a folder of this repository (ADR-021). No workflow checks them out, no POM or README points at them.
// Left on purpose: scripts/release-follow-ups.mjs, which republishes packages still bound to them (see its header).
const GONE = "krizaka-users|krizaka-platform-kit|krizaka-notifications|krizaka-accounts|krizaka-billing";
const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), "utf8");

test("no workflow checks out a deleted repository", () => {
  assert.doesNotMatch(read(".github/workflows/ci.yml"), new RegExp(`repository: krizaka/(${GONE})\\b`));
});

test("the POMs and READMEs of the folders point at krizaka-build", () => {
  for (const folder of ["krizaka-platform-kit", "krizaka-users", "krizaka-billing"]) {
    for (const file of ["pom.xml", "README.md"]) {
      assert.doesNotMatch(read(`${folder}/${file}`), new RegExp(`github\\.com[/:]krizaka/(${GONE})(?![-\\w])`), `${folder}/${file}`);
    }
  }
});
