import { test } from "node:test";
import assert from "node:assert/strict";
import { schemaRows } from "../lib/json-schema.ts";

test("one row per property, required and constraints included", () => {
  const rows = schemaRows({
    type: "object",
    required: ["id"],
    properties: {
      id: { type: "string", format: "uuid", description: "The user." },
      plan: { enum: ["free", "pro"] },
      seats: { type: "integer", minimum: 1, default: 1 },
    },
  });
  assert.deepEqual(rows.map((r) => [r.path, r.type, r.required]), [
    ["id", "string", true],
    ["plan", "enum", false],
    ["seats", "integer", false],
  ]);
  assert.equal(rows[0].description, "The user.");
  assert.deepEqual(rows[0].constraints, ["uuid"]);
  assert.deepEqual(rows[1].constraints, ['"free" · "pro"']);
  assert.deepEqual(rows[2].constraints, ["≥ 1", "default 1"]);
});

test("nested objects, arrays of objects and local $ref", () => {
  const rows = schemaRows({
    $defs: { money: { type: "object", required: ["cents"], properties: { cents: { type: "integer" }, currency: { type: "string" } } } },
    type: "object",
    properties: {
      total: { $ref: "#/$defs/money", description: "What was charged." },
      lines: { type: "array", items: { type: "object", properties: { sku: { type: "string" } } } },
    },
  });
  assert.deepEqual(rows.map((r) => [r.path, r.type, r.depth]), [
    ["total", "object", 0],
    ["total.cents", "integer", 1],
    ["total.currency", "string", 1],
    ["lines", "object[]", 0],
    ["lines[].sku", "string", 1],
  ]);
  assert.equal(rows[0].description, "What was charged.");
  assert.equal(rows[1].required, true);
});

test("an unknown $ref is shown as is, never thrown", () => {
  assert.deepEqual(schemaRows({ properties: { x: { $ref: "https://example.com/x.json" } } }).map((r) => r.type), ["any"]);
});
