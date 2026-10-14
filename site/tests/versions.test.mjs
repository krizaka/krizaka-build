import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdirSync, writeFileSync, existsSync, readFileSync } from "node:fs";
import { remarkVersions } from "../lib/remark-versions.ts";

const file = new URL("../lib/generated/maven-versions.json", import.meta.url);
if (!existsSync(file)) {
  mkdirSync(new URL("../lib/generated/", import.meta.url), { recursive: true });
  writeFileSync(file, JSON.stringify({ maven: { "krizaka-bom": "9.9.9", "krizaka-users-client": "9.9.8" } }));
}
const known = JSON.parse(readFileSync(file, "utf8")).maven;

const tree = (value) => ({ type: "root", children: [{ type: "paragraph", children: [{ type: "text", value }, { type: "inlineCode", value }] }, { type: "code", value }] });

test("a version token becomes the release Maven Central lists, in text, inline code and code blocks", () => {
  const t = tree("<version>%krizaka-bom%</version>");
  remarkVersions()(t, { path: "page.mdx" });
  const expected = `<version>${known["krizaka-bom"]}</version>`;
  assert.equal(t.children[0].children[0].value, expected);
  assert.equal(t.children[0].children[1].value, expected);
  assert.equal(t.children[1].value, expected);
});

test("a token for an artifact Central does not list fails the build", () => {
  assert.throws(() => remarkVersions()(tree("%krizaka-nope%"), { path: "page.mdx" }), /krizaka-nope/);
});

test("a percent sign in prose is left alone", () => {
  const t = tree("10 % trace sampling and 5 % more");
  remarkVersions()(t, {});
  assert.equal(t.children[1].value, "10 % trace sampling and 5 % more");
});
