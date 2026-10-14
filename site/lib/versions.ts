/* The versions read from Maven Central at build (scripts/gen-registry-versions.mjs → lib/generated/maven-versions.json). */

import { existsSync, readFileSync } from "node:fs";
import path from "node:path";

const FILE = path.join(process.cwd(), "lib", "generated", "maven-versions.json");

let cache: Record<string, string> | null = null;

/** Every `com.krizaka` artifact on Maven Central with its latest release. */
export function mavenVersions(): Record<string, string> {
  if (!cache) {
    if (!existsSync(FILE)) throw new Error("lib/generated/maven-versions.json is missing — run `npm run gen`");
    cache = (JSON.parse(readFileSync(FILE, "utf8")) as { maven: Record<string, string> }).maven;
  }
  return cache;
}

/** The release of one artifact on Central; `null` when it was never published. */
export const mavenVersion = (artifactId: string): string | null => mavenVersions()[artifactId] ?? null;
