import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { familyLabels, siteById } from "@krizaka/ui/sites";

const read = (f) => readFileSync(new URL(`../${f}`, import.meta.url), "utf8");

test("the bar and the footer are the family's (@krizaka/ui 3.10): current = java, no hand-made links to other sites", () => {
  const bar = read("app/components/TopBar.tsx");
  assert.equal((bar.match(/<NavBar\.Family current="java"/g) ?? []).length, 2, "desktop and phone panel");
  assert.match(bar, /<NavBar\.Badge version=/);
  assert.match(bar, /<NavBar\.Repo /);
  assert.match(bar, /<NavBar\.Language /, "the family's language switch (@krizaka/ui 3.26), not a hand-made button");
  assert.match(bar, /<NavBar\.MobilePreferences /);
  assert.doesNotMatch(bar, /KRIZAKA_URL|UI_URL|OPEN_SOURCE_URL|GITHUB_ORG_URL/);
  assert.match(read("app/components/FamilyFooterView.tsx"), /current="java"/);
});

test("the family data knows this site and both languages", () => {
  assert.equal(siteById("java").url, "https://java.krizaka.com");
  assert.equal(siteById("java").repo.endsWith("krizaka-build"), true);
  assert.ok(familyLabels.en.family && familyLabels.fr.family);
});
