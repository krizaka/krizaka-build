#!/usr/bin/env node
/* The content contract of the site (run by `npm run lint` and tests/content.test.mjs): every page exists in both languages,
   is listed in both meta files, has a title and a description, and the French page keeps what must not be translated —
   the same code blocks (count and language), the same version tokens, the same components, the same internal links. */
import { readdirSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const DIR = join(dirname(fileURLToPath(import.meta.url)), "..", "content", "docs");

export function checkContent(dir = DIR) {
  const errors = [];
  const files = readdirSync(dir);
  const en = files.filter((f) => /^[a-z0-9-]+\.mdx$/.test(f)).map((f) => f.replace(".mdx", ""));
  const fr = files.filter((f) => /^[a-z0-9-]+\.fr\.mdx$/.test(f)).map((f) => f.replace(".fr.mdx", ""));
  for (const slug of en) if (!fr.includes(slug)) errors.push(`${slug}: no French page (${slug}.fr.mdx)`);
  for (const slug of fr) if (!en.includes(slug)) errors.push(`${slug}.fr.mdx: no English page`);
  for (const [name, locale] of [["meta.json", "en"], ["meta.fr.json", "fr"]]) {
    const listed = JSON.parse(readFileSync(join(dir, name), "utf8")).pages.filter((p) => !p.startsWith("---"));
    for (const slug of en) if (!listed.includes(slug)) errors.push(`${name} (${locale}) does not list ${slug}`);
    for (const slug of listed) if (!en.includes(slug)) errors.push(`${name} (${locale}) lists ${slug}, which has no page`);
  }
  const facts = (src) => ({
    fences: [...src.matchAll(/^```(\w*)/gm)].map((m) => m[1]).join(","),
    tokens: [...src.matchAll(/%(krizaka-[a-z0-9-]+)%/g)].map((m) => m[1]).sort().join(","),
    components: [...src.matchAll(/^<([A-Z]\w+)/gm)].map((m) => m[1]).join(","),
    links: [...src.matchAll(/\]\((\/[^)#]*)/g)].map((m) => m[1]).sort().join(","),
  });
  for (const slug of en.filter((s) => fr.includes(s))) {
    const a = readFileSync(join(dir, `${slug}.mdx`), "utf8");
    const b = readFileSync(join(dir, `${slug}.fr.mdx`), "utf8");
    for (const [src, file] of [[a, `${slug}.mdx`], [b, `${slug}.fr.mdx`]]) {
      const head = src.match(/^---\n([\s\S]*?)\n---\n/)?.[1] ?? "";
      for (const key of ["title", "description"]) if (!new RegExp(`^${key}:\\s*\\S`, "m").test(head)) errors.push(`${file}: no ${key} in the front matter`);
      if (/```mermaid/.test(src)) errors.push(`${file}: Mermaid is not used on this site`);
    }
    const fa = facts(a), fb = facts(b);
    for (const key of Object.keys(fa)) if (fa[key] !== fb[key]) errors.push(`${slug}: the French page differs in its ${key} (${fa[key] || "none"} ≠ ${fb[key] || "none"})`);
  }
  return errors;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const errors = checkContent();
  if (errors.length) {
    console.error(errors.map((e) => `check-content: ${e}`).join("\n"));
    process.exit(1);
  }
  console.log("check-content: every page is in both languages and consistent");
}
