/* What a BOM manages: its dependencyManagement's com.krizaka artifacts with the version each resolves to, from the BOM's
   own <properties> and `${project.version}`. Pure (tests/project.test.mjs). A version it cannot resolve is left out — the
   generator never offers an artifact at a guessed version. */

const tag = (xml, name) => xml.match(new RegExp(`<${name}>([^<]*)</${name}>`))?.[1]?.trim();

/** @returns {Record<string, string>} artifactId → resolved version */
export function bomManaged(pom) {
  const xml = pom.replace(/<!--[\s\S]*?-->/g, "");
  const parent = xml.match(/<parent>([\s\S]*?)<\/parent>/)?.[1] ?? "";
  const own = xml.replace(/<parent>[\s\S]*?<\/parent>/, "");
  // `${project.version}`: the BOM's own <version>, else the one it inherits from its parent.
  const projectVersion = tag(own.replace(/<dependencyManagement>[\s\S]*<\/dependencyManagement>/, "").replace(/<properties>[\s\S]*?<\/properties>/, ""), "version") ?? tag(parent, "version");
  const properties = Object.fromEntries([...(own.match(/<properties>([\s\S]*?)<\/properties>/)?.[1] ?? "").matchAll(/<([\w.-]+)>([^<]*)<\/\1>/g)].map(([, k, v]) => [k, v.trim()]));
  const resolve = (v) =>
    v?.replace(/\$\{([^}]+)\}/g, (_, name) => (name === "project.version" ? (projectVersion ?? "\0") : (properties[name] ?? "\0")));
  const managed = own.match(/<dependencyManagement>([\s\S]*?)<\/dependencyManagement>/)?.[1] ?? "";
  const out = {};
  for (const [, dep] of managed.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)) {
    if (tag(dep, "groupId") !== "com.krizaka") continue;
    const id = tag(dep, "artifactId");
    const version = resolve(tag(dep, "version"));
    if (id && version && !version.includes("\0") && !version.includes("${")) out[id] = version;
  }
  return out;
}
