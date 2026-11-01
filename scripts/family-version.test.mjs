// ADR-021: one family version. Every pom of the reactor that names a krizaka-* parent or dependency by a literal
// version names the family version (the one release-please keeps in .release-please-manifest.json), so the reactor
// never asks Central for a released parent.
import { test } from "node:test";
import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const family = JSON.parse(readFileSync(join(root, ".release-please-manifest.json"), "utf8"))["."];
const SKIP = new Set(["node_modules", "target", ".git", "site"]);

function poms(dir) {
  return readdirSync(dir).flatMap((name) => {
    if (SKIP.has(name)) return [];
    const path = join(dir, name);
    if (statSync(path).isDirectory()) return poms(path);
    return name === "pom.xml" ? [path] : [];
  });
}

const reference = /<(parent|dependency|plugin)>\s*<groupId>com\.krizaka<\/groupId>\s*<artifactId>(krizaka-[\w-]+)<\/artifactId>\s*<version>([^<]+)<\/version>/g;

test("the family version is a plain version", () => assert.match(family, /^\d+\.\d+\.\d+$/));

test("no pom names a krizaka-* parent, dependency or plugin by a literal version other than the family's", () => {
  const wrong = [];
  for (const pom of poms(root)) {
    const xml = readFileSync(pom, "utf8").replace(/<!--[\s\S]*?-->/g, "");
    for (const [, kind, artifact, version] of xml.matchAll(reference)) {
      if (version.startsWith("${")) continue;
      if (version !== family) wrong.push(`${relative(root, pom)}: ${kind} ${artifact} ${version} (family ${family})`);
    }
  }
  assert.deepEqual(wrong, []);
});
