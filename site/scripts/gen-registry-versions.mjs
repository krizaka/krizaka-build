#!/usr/bin/env node
/* The versions the site states, read from Maven Central at build time — one source, never typed in a page.

   Every artifact under com/krizaka on repo1 (its directory listing), then each maven-metadata.xml's <release> — the
   authoritative file, not the search index (which lags). Writes the git-ignored lib/generated/maven-versions.json:
   { maven: { "krizaka-bom": "0.2.0", … }, fetchedAt }.
   When Central cannot be reached the previous file is kept; without one the build fails: a page must not state a
   version it could not read. Usage: node scripts/gen-registry-versions.mjs */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const OUT = join(dirname(fileURLToPath(import.meta.url)), "..", "lib", "generated", "maven-versions.json");
const BASE = "https://repo1.maven.org/maven2/com/krizaka/";
const get = async (url) => {
  const res = await fetch(url, { headers: { "user-agent": "krizaka-java-site-build" }, signal: AbortSignal.timeout(10_000) });
  if (!res.ok) throw new Error(`${url} answered ${res.status}`);
  return res.text();
};

async function mavenVersions() {
  const listing = await get(BASE);
  const artifacts = [...listing.matchAll(/href="([a-z0-9-]+)\/"/g)].map((m) => m[1]);
  const entries = await Promise.all(
    artifacts.map(async (a) => {
      const xml = await get(`${BASE}${a}/maven-metadata.xml`).catch(() => "");
      return [a, xml.match(/<release>([^<]+)<\/release>/)?.[1] ?? xml.match(/<latest>([^<]+)<\/latest>/)?.[1]];
    }),
  );
  return Object.fromEntries(entries.filter(([, v]) => v));
}

try {
  const maven = await mavenVersions();
  if (!maven["krizaka-bom"]) throw new Error("krizaka-bom is not listed");
  mkdirSync(dirname(OUT), { recursive: true });
  writeFileSync(OUT, JSON.stringify({ maven, fetchedAt: new Date().toISOString() }, null, 2) + "\n");
  console.log(`registry-versions: ${Object.keys(maven).length} Maven artifacts (krizaka-bom ${maven["krizaka-bom"]})`);
} catch (error) {
  if (!existsSync(OUT)) {
    console.error(`registry-versions: Maven Central unreachable (${error.message}) and no previous file — cannot state a version`);
    process.exit(1);
  }
  console.warn(`registry-versions: Maven Central unreachable (${error.message}) — keeping ${JSON.parse(readFileSync(OUT, "utf8")).fetchedAt}`);
}
