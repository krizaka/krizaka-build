#!/usr/bin/env node
// After a release tag, publish what the tag's own run cannot: the platform kit and users were first published from their own
// repositories, so GitHub Packages answers HTTP 422 to any other (ci.yml `deploy-repository`). Then check that every artifact is served.
//
//   node scripts/release-follow-ups.mjs plan
//   node scripts/release-follow-ups.mjs dispatch <tag>          one ci.yml run per group, one after the other
//   node scripts/release-follow-ups.mjs check-published <tag>   polls the registry, fails when an artifact is still missing
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

export const GROUPS = [
  { directory: "krizaka-platform-kit", repository: "krizaka/krizaka-platform-kit" },
  { directory: "krizaka-users", repository: "krizaka/krizaka-users" },
];

const withoutParent = (pom) => pom.replace(/<parent>[\s\S]*?<\/parent>/, "");
const tag = (pom, name) => pom.match(new RegExp(`<${name}>([^<]+)</${name}>`))?.[1].trim();

/** Every pom of a group, aggregator first, as { path, pom } (path relative to the repository root, usable by `mvn -pl`). */
export function walk(root, path) {
  const pom = readFileSync(join(root, path, "pom.xml"), "utf8");
  const modules = [...pom.matchAll(/<module>([^<]+)<\/module>/g)].map((m) => m[1]);
  return [{ path, pom }, ...modules.flatMap((m) => walk(root, `${path}/${m}`))];
}

const skipsDeploy = ({ pom }) => /<maven\.deploy\.skip>\s*true\s*</.test(pom);

/** Modules that reach the registry: a runnable service or an example sets maven.deploy.skip. */
export function publishableModules(entries) {
  return entries.filter((entry) => !skipsDeploy(entry)).map(({ path }) => path);
}

export function followUpGroups(root) {
  return GROUPS.map(({ directory, repository }) => {
    const entries = walk(root, directory).filter((entry) => !skipsDeploy(entry));
    return { repository, entries, modules: entries.map(({ path }) => path) };
  });
}

export function publishedPomUrl(repository, group, artifact, version) {
  return `https://maven.pkg.github.com/${repository}/${group.replaceAll(".", "/")}/${artifact}/${version}/${artifact}-${version}.pom`;
}

// An absolute path: the lookup never walks a writable PATH (GH_BIN overrides it, e.g. /opt/homebrew/bin/gh on a Mac).
const GH_BIN = process.env.GH_BIN ?? "/usr/bin/gh";
const gh = (...args) => execFileSync(GH_BIN, args, { encoding: "utf8" }).trim();
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const root = join(dirname(fileURLToPath(import.meta.url)), "..");

/** Dispatch ci.yml on the tag for one group and wait for that run, so the next group never replaces a pending run of the same concurrency group. */
async function dispatchAndWait(repo, ref, group) {
  const since = new Date(Date.now() - 5000).toISOString();
  gh("workflow", "run", "ci.yml", "--repo", repo, "--ref", ref, "-f", `deploy-modules=${group.modules.join(",")}`, "-f", `deploy-repository=${group.repository}`);
  let id = "";
  for (let i = 0; i < 30 && !id; i += 1) {
    await sleep(5000);
    id = gh("run", "list", "--repo", repo, "--workflow", "ci.yml", "--event", "workflow_dispatch", "--branch", ref, "--created", `>=${since}`, "--json", "databaseId", "--jq", ".[0].databaseId // empty");
  }
  if (!id) throw new Error("the republish run never started");
  console.log(`republish ${group.repository}: run ${id}`);
  gh("run", "watch", id, "--repo", repo, "--exit-status", "--interval", "30");
}

async function dispatch(ref) {
  const repo = process.env.GITHUB_REPOSITORY ?? "krizaka/krizaka-build";
  let failed = false;
  for (const group of followUpGroups(root)) {
    try {
      await dispatchAndWait(repo, ref, group);
    } catch (error) {
      failed = true;
      console.error(`::error::republish of ${group.repository}: ${error.message}`);
    }
  }
  if (failed) process.exit(1);
}

async function checkPublished(ref) {
  const version = ref.replace(/^v/, "");
  const basic = `x-access-token:${process.env.GITHUB_TOKEN ?? ""}`;
  const auth = `Basic ${Buffer.from(basic).toString("base64")}`;
  const budget = Number(process.env.PUBLISHED_WAIT_SECONDS) || 600;
  const missing = [];
  for (const { repository, entries } of followUpGroups(root)) {
    for (const { pom } of entries) {
      const own = withoutParent(pom);
      const url = publishedPomUrl(repository, tag(own, "groupId") ?? tag(pom, "groupId"), tag(own, "artifactId"), version);
      let code = 0;
      for (let waited = 0; waited <= budget; waited += 20) {
        code = (await fetch(url, { headers: { Authorization: auth } })).status;
        if (code === 200) break;
        await sleep(20000);
      }
      if (code !== 200) missing.push(`${tag(own, "artifactId")}@${version} (${repository}, HTTP ${code})`);
    }
  }
  if (missing.length) {
    console.error(`::error::on the tag but not on GitHub Packages: ${missing.join(", ")}`);
    process.exit(1);
  }
  console.log("every artifact of the platform kit and users is on GitHub Packages");
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const [command, ref] = process.argv.slice(2);
  if (command === "plan") console.log(JSON.stringify(followUpGroups(root).map(({ repository, modules }) => ({ repository, modules })), null, 2));
  else if (command === "dispatch" && ref) await dispatch(ref);
  else if (command === "check-published" && ref) await checkPublished(ref);
  else {
    console.error("usage: release-follow-ups.mjs plan | dispatch <tag> | check-published <tag>");
    process.exit(2);
  }
}
