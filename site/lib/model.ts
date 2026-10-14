/* The building blocks map: the artifacts of the Krizaka Java repositories, read from the POMs (scripts/gen-building-blocks.mjs
   → lib/generated/building-blocks.json), with the release each has on Maven Central (lib/versions.ts). The model is
   checked here at build: the roles the map draws, a known repository for every artifact, relations between known
   artifacts. A page that cannot be right does not build — a fact the map lacks is added to the generator, never here. */

import data from "./generated/building-blocks.json";
import { mavenVersion } from "./versions";

import { ROLES, type MapData, type MapDependency, type MapModule, type RoleId } from "./roles";

export { ROLES };
export type { MapData, MapDependency, MapModule, RoleId };

const fail = (message: string): never => {
  throw new Error(`building blocks map: ${message}`);
};

export function buildingBlocksMap(): MapData {
  const roles = data.roles.map((r) => r.id);
  if (roles.join() !== ROLES.join()) fail(`the generator has the roles ${roles.join(", ")}, the map draws ${ROLES.join(", ")}`);
  const repositories = new Map(data.repositories.map((r) => [r.name, r.url]));
  const modules: MapModule[] = data.modules.map((m) => ({
    id: m.id,
    role: m.role as RoleId,
    repository: m.repository,
    repositoryUrl: repositories.get(m.repository) ?? fail(`artifact ${m.id} names an unknown repository ${m.repository}`),
    path: m.path,
    packaging: m.packaging,
    version: m.version ?? fail(`artifact ${m.id} has no version`),
    central: mavenVersion(m.id),
    description: m.description,
  }));
  const known = new Set(modules.map((m) => m.id));
  const dependencies: MapDependency[] = data.dependencies.map((d) => {
    if (!known.has(d.from) || !known.has(d.to)) fail(`a relation names an unknown artifact (${d.from} → ${d.to})`);
    return { from: d.from, to: d.to, kind: d.kind, indirect: !["compile", "runtime", "provided"].includes(d.kind) };
  });
  return { modules, dependencies };
}

/** The artifacts a page may focus (the `focus` of <BuildingBlocksMap>), checked against the model. */
export function checkFocus(id: string): string {
  if (!buildingBlocksMap().modules.some((m) => m.id === id)) fail(`a page focuses ${id}, which is not an artifact of the POMs`);
  return id;
}
