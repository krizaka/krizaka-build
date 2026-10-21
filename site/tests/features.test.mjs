import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { checkFeatures } from "../scripts/check-features.mjs";

// The features matrix is honest by construction: a claim the code does not back fails the build.
const data = JSON.parse(readFileSync(new URL("../lib/features.json", import.meta.url), "utf8"));
const en = JSON.parse(readFileSync(new URL("../messages/en.json", import.meta.url), "utf8"));

const repo = mkdtempSync(join(tmpdir(), "features-"));
mkdirSync(join(repo, "src"), { recursive: true });
writeFileSync(join(repo, "src", "Proof.java"), "class Proof {}");
const words = { features: { items: { a: { name: "A", note: "a" } } } };
const context = { dirs: new Map([["krizaka-x", repo]]), messages: { en: words, fr: words }, pages: ["web"] };
const matrix = (feature) => ({ statuses: data.statuses, features: [{ id: "a", ...feature }] });

test("a shipped line whose files exist, with its words, passes", () => {
  assert.deepEqual(checkFeatures(matrix({ status: "shipped", page: "/web", evidence: [{ repository: "krizaka-x", path: "src/Proof.java" }] }), context), []);
});

test("a shipped line whose file is not on main fails", () => {
  const errors = checkFeatures(matrix({ status: "shipped", evidence: [{ repository: "krizaka-x", path: "src/Missing.java" }] }), context);
  assert.match(errors.join(), /does not exist on main/);
});

test("shipped without evidence, planned with evidence, partial or planned without an issue fail", () => {
  assert.match(checkFeatures(matrix({ status: "shipped" }), context).join(), /without evidence/);
  assert.match(checkFeatures(matrix({ status: "planned", issue: "https://github.com/krizaka/x/issues/1", evidence: [{ repository: "krizaka-x", path: "src/Proof.java" }] }), context).join(), /is it partial/);
  assert.match(checkFeatures(matrix({ status: "planned" }), context).join(), /without its issue/);
});

test("a line without its words, or a link to a page that does not exist, fails", () => {
  const silent = { ...context, messages: { en: words, fr: { features: { items: {} } } } };
  assert.match(checkFeatures(matrix({ status: "planned", issue: "https://github.com/krizaka/x/issues/1" }), silent).join(), /messages\/fr\.json/);
  assert.match(checkFeatures(matrix({ status: "planned", issue: "https://github.com/krizaka/x/issues/1", page: "/nowhere" }), context).join(), /not a page/);
});

test("the matrix itself: every line has its words in English", () => {
  for (const f of data.features) assert.ok(en.features.items[f.id]?.name, f.id);
});
