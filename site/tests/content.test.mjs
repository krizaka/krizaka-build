import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { checkContent } from "../scripts/check-content.mjs";

const page = (body, title = "T") => `---\ntitle: ${title}\ndescription: D\n---\n\n${body}\n`;
function fixture(files) {
  const dir = mkdtempSync(join(tmpdir(), "kz-java-content-"));
  for (const [name, content] of Object.entries(files)) writeFileSync(join(dir, name), content);
  return dir;
}
const meta = (pages) => JSON.stringify({ pages });

test("the real content is in both languages and consistent", () => {
  assert.deepEqual(checkContent(), []);
});

test("a page without its French twin is refused", () => {
  const dir = fixture({ "index.mdx": page("x"), "meta.json": meta(["index"]), "meta.fr.json": meta(["index"]) });
  assert.match(checkContent(dir).join("\n"), /no French page/);
});

test("a page missing from a meta file is refused", () => {
  const dir = fixture({ "index.mdx": page("x"), "index.fr.mdx": page("x"), "meta.json": meta([]), "meta.fr.json": meta(["index"]) });
  assert.match(checkContent(dir).join("\n"), /meta\.json \(en\) does not list index/);
});

test("a French page that drops a code block, a token or a link is refused", () => {
  const en = page("[a](/web) `%krizaka-bom%`\n\n```xml\n<a/>\n```\n\n<PendingRelease />");
  const dir = fixture({ "index.mdx": en, "index.fr.mdx": page("[a](/security)"), "meta.json": meta(["index"]), "meta.fr.json": meta(["index"]) });
  const out = checkContent(dir).join("\n");
  assert.match(out, /fences/);
  assert.match(out, /components/);
  assert.match(out, /links/);
});

test("a page without a description is refused", () => {
  const dir = fixture({ "index.mdx": "---\ntitle: T\n---\n\nx", "index.fr.mdx": page("x"), "meta.json": meta(["index"]), "meta.fr.json": meta(["index"]) });
  assert.match(checkContent(dir).join("\n"), /no description/);
});
