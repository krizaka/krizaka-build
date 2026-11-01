#!/usr/bin/env node
// Guard (ADR-021): a release is « released » only when every module is on GitHub Packages.
//
// release-please tags the release as soon as its pull request merges; the deploy is a separate workflow that can fail
// (2026-11-01: v0.6.0, HTTP 422 on the platform-kit packages, still linked to krizaka/krizaka-platform-kit). This script reads
// the reactor (root pom.xml, <module> recursively), drops the modules that do not ship (maven.deploy.skip=true) and asks the
// registry for each artifact's versions. Any module missing the version: exit 1 and the list.
//
//   node scripts/check-published.mjs <vX.Y.Z> [--org krizaka] [--group com.krizaka]
// GITHUB_TOKEN needs `packages: read`.
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const tag = (re, xml) => re.exec(xml)?.[1]?.trim();

/** [{ artifactId, dir }] of the modules that ship: the reactor walked from `root`, minus maven.deploy.skip=true. */
export function publishable(root, read = (p) => readFileSync(p, "utf8"), exists = existsSync) {
  const out = [];
  // A module inherits the skip only from the POM it names as its <parent> (the reactor root is not the parent of krizaka-parent).
  const walk = (dir, from) => {
    const file = join(dir, "pom.xml");
    if (!exists(file)) return;
    const xml = read(file).replace(/<!--[\s\S]*?-->/g, "");
    const own = tag(/<maven\.deploy\.skip>([^<]+)<\/maven\.deploy\.skip>/, xml);
    const parent = tag(/<parent>[\s\S]*?<artifactId>([^<]+)<\/artifactId>/, xml);
    const skip = own === undefined ? from?.artifactId === parent && from.skip : own === "true";
    const artifactId = tag(/<artifactId>([^<]+)<\/artifactId>/, xml.replace(/<parent>[\s\S]*?<\/parent>/, ""));
    if (!skip && artifactId) out.push({ artifactId, dir });
    for (const [, m] of xml.matchAll(/<module>([^<]+)<\/module>/g)) walk(join(dir, m.trim()), { artifactId, skip });
  };
  walk(root, undefined);
  return out;
}

/** The artifacts whose versions do not include `version`: [{ artifactId, why }]. */
export async function missing(artifacts, version, versionsOf) {
  const gaps = [];
  for (const { artifactId } of artifacts) {
    const versions = await versionsOf(artifactId);
    if (versions === null) gaps.push({ artifactId, why: "package not found" });
    else if (!versions.includes(version)) gaps.push({ artifactId, why: `versions: ${versions.slice(0, 3).join(", ") || "none"}` });
  }
  return gaps;
}

function registry(org, group, token) {
  const headers = { Accept: "application/vnd.github+json", ...(token ? { Authorization: `Bearer ${token}` } : {}) };
  return async (artifactId) => {
    const names = [];
    for (let page = 1; ; page += 1) {
      const res = await fetch(`https://api.github.com/orgs/${org}/packages/maven/${group}.${artifactId}/versions?per_page=100&page=${page}`, { headers });
      if (res.status === 404) return page === 1 ? null : names;
      if (!res.ok) throw new Error(`${artifactId}: HTTP ${res.status}`);
      const batch = (await res.json()).map((v) => v.name);
      names.push(...batch);
      if (batch.length < 100) return names;
    }
  };
}

async function main(argv) {
  const flag = (n, d) => { const i = argv.indexOf(`--${n}`); return i >= 0 ? argv[i + 1] : d; };
  const version = argv.find((a) => /^v?\d+\.\d+\.\d+$/.test(a))?.replace(/^v/, "");
  if (!version) { console.error("usage: check-published.mjs <vX.Y.Z> [--org krizaka] [--group com.krizaka]"); return 2; }
  const root = join(dirname(fileURLToPath(import.meta.url)), "..");
  const artifacts = publishable(root);
  const gaps = await missing(artifacts, version, registry(flag("org", "krizaka"), flag("group", "com.krizaka"), process.env.GITHUB_TOKEN));
  if (gaps.length === 0) { console.log(`v${version}: ${artifacts.length} modules are on GitHub Packages`); return 0; }
  console.error(`v${version} is NOT fully published: ${gaps.length} of ${artifacts.length} modules missing`);
  for (const g of gaps) console.error(`  - ${g.artifactId} (${g.why})`);
  return 1;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) process.exit(await main(process.argv.slice(2)));
