import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { REPOSITORIES, ROLES } from "../scripts/gen-building-blocks.mjs";

// The generated map (scripts/gen-building-blocks.mjs, `npm run gen`): the shape the site relies on.
const data = JSON.parse(readFileSync(new URL("../lib/generated/building-blocks.json", import.meta.url), "utf8"));
const ids = new Set(data.modules.map((m) => m.id));

test("the build is on the map: parent, BOM and test kit", () => {
  for (const id of ["krizaka-parent", "krizaka-bom", "krizaka-test-support"]) {
    assert.equal(data.modules.find((m) => m.id === id)?.role, "build", id);
  }
});

test("every artifact has a known role, repository and version", () => {
  const roles = new Set(ROLES.map((r) => r.id));
  for (const m of data.modules) {
    assert.ok(roles.has(m.role), `${m.id}: role ${m.role}`);
    assert.ok(REPOSITORIES.includes(m.repository), `${m.id}: repository ${m.repository}`);
    assert.ok(m.version, `${m.id}: no version`);
  }
});

test("every relation joins two known artifacts, and the kinds are the ones the map draws", () => {
  const kinds = new Set(["compile", "runtime", "provided", "test", "parent", "import"]);
  assert.ok(data.dependencies.length > 0);
  for (const d of data.dependencies) {
    assert.ok(ids.has(d.from) && ids.has(d.to), `${d.from} → ${d.to}`);
    assert.ok(kinds.has(d.kind), `${d.from} → ${d.to}: ${d.kind}`);
  }
});

test("every artifact inherits krizaka-parent through its own parent chain (the BOM is imported, the parent is the root)", () => {
  const parents = new Map(data.dependencies.filter((d) => d.kind === "parent").map((d) => [d.from, d.to]));
  const root = (id) => { let cur = id; for (let i = 0; i < 10 && parents.has(cur); i++) cur = parents.get(cur); return cur; };
  const orphans = data.modules.filter((m) => m.id !== "krizaka-parent" && root(m.id) !== "krizaka-parent").map((m) => m.id);
  assert.deepEqual(orphans, []);
});
