import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { test } from "node:test";
import { buildInfo } from "../scripts/build-info.mjs";

const read = (f) => readFileSync(new URL(`../${f}`, import.meta.url), "utf8");
const sha = "0123456789abcdef0123456789abcdef01234567";

test("the build id is the commit, or « local » when none is bound", () => {
  assert.deepEqual(buildInfo(sha.toUpperCase()), { commit: sha });
  assert.deepEqual(buildInfo(undefined), { commit: "local" });
  assert.deepEqual(buildInfo("not-a-sha"), { commit: "local" });
});

test("the deploy binds APP_COMMIT at build time and /build.json serves it statically", () => {
  assert.match(readFileSync(new URL("../../.do/app.yaml", import.meta.url), "utf8"), /key: APP_COMMIT\s+value: \$\{_self\.COMMIT_HASH\}\s+scope: BUILD_TIME/);
  assert.match(read("app/build.json/route.ts"), /dynamic = "force-static"/);
  assert.match(read("app/build.json/route.ts"), /process\.env\.APP_COMMIT/);
});

test("the post-deploy workflow waits for the pushed commit on the live site and the IndexNow key file is served", () => {
  const workflow = readFileSync(new URL("../../.github/workflows/post-deploy.yml", import.meta.url), "utf8");
  assert.match(workflow, /version-url: https:\/\/java\.krizaka\.com\/build\.json/);
  assert.match(workflow, /expect: \$\{\{ github\.sha \}\}/);
  const id = /indexnow-key: ([0-9a-f]{32})/.exec(workflow)?.[1];
  assert.ok(id, "an IndexNow key");
  assert.ok(readdirSync(new URL("../public/", import.meta.url)).includes(`${id}.txt`));
  assert.equal(read(`public/${id}.txt`).trim(), id);
});
