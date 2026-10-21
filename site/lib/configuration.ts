/* The configuration reference: every krizaka.* property of Krizaka Java, read from the metadata the configuration processor
   generates (scripts/gen-configuration.mjs → lib/generated/configuration.json). A page that names an artifact without
   properties does not build. */

import data from "./generated/configuration.json";

export type ConfigProperty = { name: string; type: string; description: string; defaultValue?: unknown };
export type ConfigHint = { name: string; values: { value: string; description: string }[] };
export type ConfigModule = { artifact: string; repository: string; repositoryUrl: string; properties: ConfigProperty[]; hints: ConfigHint[] };

export function configurationModules(): ConfigModule[] {
  return data.repositories.flatMap((r) =>
    r.modules.map((m) => ({ artifact: m.artifact, repository: r.name, repositoryUrl: r.url, properties: m.properties as ConfigProperty[], hints: (m.hints ?? []) as ConfigHint[] })),
  );
}

/** The artifact a page shows (`<ConfigurationReference artifact="…" />`), checked against the reference. */
export function checkArtifact(artifact: string): string {
  if (!configurationModules().some((m) => m.artifact === artifact)) throw new Error(`configuration reference: ${artifact} declares no property`);
  return artifact;
}

/** A Java type as a reader says it: `java.util.List<java.lang.String>` → `List<String>`, a nested enum → its name. */
export function shortType(type: string): string {
  return type.replace(/\b(?:[a-z_]\w*\.)+([A-Z]\w*)/g, "$1").replace(/\b\w+\$(\w+)/g, "$1");
}

/** A default as it is written in application.yml; `null` when the property has none. */
export function formatDefault(value: unknown): string | null {
  if (value === undefined || value === null) return null;
  if (Array.isArray(value)) return value.join(", ");
  return String(value);
}
