import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync, readdirSync, statSync } from "node:fs";

/* One footer, one contentinfo: @krizaka/ui's SiteFooter, rendered once by the root after the docs layout. No page, doc
   template or component renders another (the docs layout's `nd-` shell has no footer of its own). */
const read = (f) => readFileSync(new URL(`../${f}`, import.meta.url), "utf8");
const walk = (dir) =>
  readdirSync(new URL(`../${dir}/`, import.meta.url)).flatMap((name) => {
    const rel = `${dir}/${name}`;
    return statSync(new URL(`../${rel}`, import.meta.url)).isDirectory() ? walk(rel) : [rel];
  });

test("the root renders the footer once, after the docs layout", () => {
  const root = read("app/components/SiteRoot.tsx");
  assert.equal(root.match(/<SiteFooter locale=\{locale\} \/>/g)?.length, 1);
  assert.ok(root.indexOf("</DocsLayout>") < root.indexOf("<SiteFooter "));
});

test("no other file renders a footer", () => {
  const offenders = walk("app")
    .filter((f) => /\.tsx$/.test(f) && !/SiteFooter\.tsx$|FamilyFooterView\.tsx$|SiteRoot\.tsx$/.test(f))
    .filter((f) => /<SiteFooter\b|<FamilyFooter\b|<footer\b/.test(read(f)));
  assert.deepEqual(offenders, []);
});
