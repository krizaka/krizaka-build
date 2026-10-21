#!/usr/bin/env node
/* The features matrix is honest by construction (run by `npm run lint`): a « shipped » or « partial » line names the files that
   prove it, and each must exist on `main` of its repository (the clones gen-building-blocks.mjs uses); a « partial » or
   « planned » line links the issue that tracks the rest; every line has its words in both languages; a page it links exists.
   A claim the code does not back fails the build. */
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { locate } from "./gen-building-blocks.mjs";

const SITE = join(dirname(fileURLToPath(import.meta.url)), "..");
const read = (p) => JSON.parse(readFileSync(join(SITE, p), "utf8"));

export function checkFeatures({ features, statuses }, { dirs, messages, pages }) {
  const errors = [];
  const ids = new Set();
  for (const f of features) {
    if (ids.has(f.id)) errors.push(`${f.id}: listed twice`);
    ids.add(f.id);
    if (!statuses.includes(f.status)) errors.push(`${f.id}: unknown status ${f.status}`);
    const evidence = f.evidence ?? [];
    if (f.status !== "planned" && evidence.length === 0) errors.push(`${f.id}: ${f.status} without evidence`);
    if (f.status === "planned" && evidence.length > 0) errors.push(`${f.id}: planned, yet it names evidence — is it partial?`);
    if (f.status !== "shipped" && !/^https:\/\/github\.com\/krizaka\/[\w.-]+\/issues\/\d+$/.test(f.issue ?? "")) errors.push(`${f.id}: ${f.status} without its issue`);
    for (const e of evidence) {
      const dir = dirs.get(e.repository);
      if (!dir) errors.push(`${f.id}: unknown repository ${e.repository}`);
      else if (!existsSync(join(dir, e.path))) errors.push(`${f.id}: ${e.repository}/${e.path} does not exist on main`);
    }
    if (f.page && !pages.includes(f.page.replace(/^\//, ""))) errors.push(`${f.id}: links ${f.page}, which is not a page`);
    for (const [locale, m] of Object.entries(messages)) {
      const words = m.features?.items?.[f.id];
      if (!words?.name || !words?.note) errors.push(`${f.id}: no name or note in messages/${locale}.json`);
    }
  }
  for (const [locale, m] of Object.entries(messages)) {
    for (const id of Object.keys(m.features?.items ?? {})) if (!ids.has(id)) errors.push(`messages/${locale}.json: features.items.${id} is not a feature`);
  }
  return errors;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const meta = read("content/docs/meta.json").pages.filter((p) => !p.startsWith("---"));
  const errors = checkFeatures(read("lib/features.json"), {
    dirs: locate(),
    messages: { en: read("messages/en.json"), fr: read("messages/fr.json") },
    pages: meta,
  });
  if (errors.length) {
    console.error(errors.map((e) => `check-features: ${e}`).join("\n"));
    process.exit(1);
  }
  console.log("check-features: every claim is backed by the code");
}
