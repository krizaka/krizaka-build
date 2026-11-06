// The release publishes everything by itself: after the tag, the platform kit and users are republished through their own repository
// (a package is bound to the repository it was first published from, HTTP 422 otherwise), then every published artifact is checked.
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { followUpGroups, publishedPomUrl, publishableModules } from "./release-follow-ups.mjs";

const root = new URL("..", import.meta.url).pathname;
const workflow = readFileSync(new URL("../.github/workflows/release-please.yml", import.meta.url), "utf8");

test("the groups are the platform kit and users, each with its own repository", () => {
  const groups = followUpGroups(root);
  assert.deepEqual(groups.map((g) => g.repository), ["krizaka/krizaka-platform-kit", "krizaka/krizaka-users"]);
});

test("a group lists its aggregator and nested modules as -pl paths, never a service or an example", () => {
  const [kit, users] = followUpGroups(root);
  assert.ok(kit.modules.includes("krizaka-platform-kit"));
  assert.ok(kit.modules.includes("krizaka-platform-kit/krizaka-web"));
  assert.ok(kit.modules.some((m) => m.startsWith("krizaka-platform-kit/starters/")));
  assert.ok(!kit.modules.some((m) => m.includes("examples/") || m.includes("notifications-service")));
  assert.deepEqual(users.modules.filter((m) => m.includes("service")), []);
  assert.ok(users.modules.includes("krizaka-users/krizaka-users-api"));
});

test("publishableModules skips the poms that set maven.deploy.skip", () => {
  const pom = (skip) => `<project><properties><maven.deploy.skip>${skip}</maven.deploy.skip></properties></project>`;
  assert.equal(publishableModules([{ path: "a", pom: pom("true") }, { path: "b", pom: pom("false") }]).join(), "b");
});

test("the registry URL of an artifact is the one maven.yml reads", () => {
  assert.equal(
    publishedPomUrl("krizaka/krizaka-users", "com.krizaka", "krizaka-users-api", "0.7.1"),
    "https://maven.pkg.github.com/krizaka/krizaka-users/com/krizaka/krizaka-users-api/0.7.1/krizaka-users-api-0.7.1.pom",
  );
});

test("the release workflow runs the follow-ups on a created release, then check-published", () => {
  assert.match(workflow, /release_created == 'true'/);
  assert.match(workflow, /node scripts\/release-follow-ups\.mjs dispatch "\$TAG"/);
  assert.match(workflow, /node scripts\/release-follow-ups\.mjs check-published "\$TAG"/);
  assert.ok(workflow.indexOf("release-follow-ups.mjs dispatch") < workflow.indexOf("release-follow-ups.mjs check-published"));
});

test("the follow-ups job has the permissions to dispatch and read packages", () => {
  assert.match(workflow, /follow-ups:[\s\S]*?needs: release-please[\s\S]*?actions: write[\s\S]*?packages: read/);
});
