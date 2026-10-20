#!/usr/bin/env node
/* The versions the site states, read from Maven Central at build time — one source, never typed in a page.

   Every artifact under com/krizaka on repo1 (its directory listing), then each maven-metadata.xml's <release> — the
   authoritative file, not the search index (which lags). Then the published BOM's own POM: what it manages, at which
   version, and whether Central has that exact version (the « start a project » generator offers only those). Writes the
   git-ignored lib/generated/maven-versions.json:
   { maven: { "krizaka-bom": "0.2.0", … }, bom: { version, manages: { "<artifactId>": { version, onCentral } } }, fetchedAt }.
   When Central cannot be reached the previous file is kept; without one the build fails: a page must not state a
   version it could not read. Usage: node scripts/gen-registry-versions.mjs */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { bomManaged } from "./bom-managed.mjs";

const OUT = join(dirname(fileURLToPath(import.meta.url)), "..", "lib", "generated", "maven-versions.json");
const BASE = "https://repo1.maven.org/maven2/com/krizaka/";
const get = async (url) => {
  const res = await fetch(url, { headers: { "user-agent": "krizaka-java-site-build" }, signal: AbortSignal.timeout(10_000) });
  if (!res.ok) throw new Error(`${url} answered ${res.status}`);
  return res.text();
};

/** Every artifact with its latest release and every version Central lists. */
async function mavenMetadata() {
  const listing = await get(BASE);
  const artifacts = [...listing.matchAll(/href="([a-z0-9-]+)\/"/g)].map((m) => m[1]);
  const entries = await Promise.all(
    artifacts.map(async (a) => {
      const xml = await get(`${BASE}${a}/maven-metadata.xml`).catch(() => "");
      const release = xml.match(/<release>([^<]+)<\/release>/)?.[1] ?? xml.match(/<latest>([^<]+)<\/latest>/)?.[1];
      return [a, { release, versions: [...xml.matchAll(/<version>([^<]+)<\/version>/g)].map((m) => m[1]) }];
    }),
  );
  return Object.fromEntries(entries.filter(([, v]) => v.release));
}

/** What the published BOM manages, each artifact with the version it resolves to and whether Central has it. */
async function bomOf(version, metadata) {
  const managed = bomManaged(await get(`${BASE}krizaka-bom/${version}/krizaka-bom-${version}.pom`));
  if (!Object.keys(managed).length) throw new Error(`krizaka-bom ${version} manages nothing readable`);
  return { version, manages: Object.fromEntries(Object.entries(managed).map(([id, v]) => [id, { version: v, onCentral: Boolean(metadata[id]?.versions.includes(v)) }])) };
}

try {
  const metadata = await mavenMetadata();
  const maven = Object.fromEntries(Object.entries(metadata).map(([id, m]) => [id, m.release]));
  if (!maven["krizaka-bom"]) throw new Error("krizaka-bom is not listed");
  const bom = await bomOf(maven["krizaka-bom"], metadata);
  mkdirSync(dirname(OUT), { recursive: true });
  writeFileSync(OUT, JSON.stringify({ maven, bom, fetchedAt: new Date().toISOString() }, null, 2) + "\n");
  const ready = Object.values(bom.manages).filter((m) => m.onCentral).length;
  console.log(`registry-versions: ${Object.keys(maven).length} Maven artifacts (krizaka-bom ${maven["krizaka-bom"]}, manages ${Object.keys(bom.manages).length}, ${ready} on Central at that version)`);
} catch (error) {
  if (!existsSync(OUT)) {
    console.error(`registry-versions: Maven Central unreachable (${error.message}) and no previous file — cannot state a version`);
    process.exit(1);
  }
  console.warn(`registry-versions: Maven Central unreachable (${error.message}) — keeping ${JSON.parse(readFileSync(OUT, "utf8")).fetchedAt}`);
}
