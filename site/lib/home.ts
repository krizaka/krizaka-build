/* The home's structure — which artifacts each family of building blocks is, and where its guide is. The words are
   messages (home.families.<id>); the artifacts are checked against the model generated from the POMs, so a family that
   names an artifact the POMs do not have fails the build. Versions are read from Maven Central, never typed. */

import { FAMILIES, FAMILY_IDS, type FamilyId } from "./families";
import { buildingBlocksMap } from "./model";
import { mavenVersion } from "./versions";

export interface Family {
  id: FamilyId;
  module: string;
  starter: string | null;
  guide: string;
  bird: "falcon" | "pigeon" | null;
  /** The release on Maven Central; `null` while the module is in the repository but not published (said honestly on the card). */
  central: string | null;
}

/** The families with the release Central lists, each artifact checked against the POMs' model. */
export function families(): Family[] {
  const known = new Set(buildingBlocksMap().modules.map((m) => m.id));
  return FAMILY_IDS.map((id) => {
    const f = FAMILIES[id];
    for (const artifact of [f.module, f.starter]) if (artifact && !known.has(artifact)) throw new Error(`home: the family ${id} names ${artifact}, which is not an artifact of the POMs`);
    return { id, ...f, central: mavenVersion(f.module) };
  });
}

/** The modules a starter switches on, read from the model: the published starters with the library module each pulls (compile). */
export function starterModules(): { starter: string; module: string }[] {
  const { modules, dependencies } = buildingBlocksMap();
  return modules
    .filter((m) => m.role === "starter" && m.central)
    .map((s) => ({ starter: s.id, module: dependencies.find((d) => d.from === s.id && d.kind === "compile" && modules.find((m) => m.id === d.to)?.role === "platform")?.to ?? "" }))
    .filter((x) => x.module);
}

/** The figures of the hero: the library modules and starters that are on Maven Central (not the ones still in a repository),
    and every artifact the POMs declare. */
export function counts() {
  const { modules } = buildingBlocksMap();
  const published = (role: string) => modules.filter((m) => m.role === role && m.central).length;
  return { modules: published("platform"), starters: published("starter"), artifacts: modules.length };
}
