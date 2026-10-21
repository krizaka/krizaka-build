import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { CONFIG_REPOSITORIES, checkReference } from "../scripts/gen-configuration.mjs";

// The configuration reference (scripts/gen-configuration.mjs, `npm run gen`): generated from each repository's metadata.
const data = JSON.parse(readFileSync(new URL("../lib/generated/configuration.json", import.meta.url), "utf8"));
const properties = data.repositories.flatMap((r) => r.modules).flatMap((m) => m.properties);

const reference = (properties) => ({ repository: "krizaka-x", modules: [{ artifact: "krizaka-x", properties, hints: [] }] });

test("every repository with properties is read, in order", () => {
  assert.deepEqual(data.repositories.map((r) => r.name), CONFIG_REPOSITORIES);
});

test("the kit's properties are there, described, with their defaults", () => {
  const byName = new Map(properties.map((p) => [p.name, p]));
  assert.equal(byName.get("krizaka.messaging.outbox.lease")?.defaultValue, "2m");
  assert.equal(byName.get("krizaka.web.problems.base-type")?.defaultValue, "https://krizaka.com/problems/");
  assert.ok(byName.has("krizaka.security.jwt.secret"), "krizaka-security generates its metadata");
  assert.ok(byName.has("krizaka.users.client.base-url") && byName.has("krizaka.billing.read-timeout"));
  for (const p of properties) assert.ok(p.description && !p.description.includes("{@"), `${p.name}: ${p.description}`);
});

test("a reference that names another repository, repeats a property or keeps Javadoc is refused", () => {
  assert.throws(() => checkReference(reference([]), "krizaka-y"), /names the repository/);
  const p = { name: "krizaka.x.a", type: "java.lang.String", description: "A." };
  assert.throws(() => checkReference(reference([p, p]), "krizaka-x"), /declared twice/);
  assert.throws(() => checkReference(reference([{ ...p, description: "the {@code x}" }]), "krizaka-x"), /written for the reader/);
  assert.throws(() => checkReference(reference([{ ...p, name: "spring.x" }]), "krizaka-x"), /not a krizaka/);
  assert.equal(checkReference(reference([p]), "krizaka-x").modules.length, 1);
});
