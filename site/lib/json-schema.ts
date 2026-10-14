/* JSON Schema → rows of a table (EventSchema). Pure: no React, no I/O — tested by tests/json-schema.test.mjs.
   Covers what event contracts use (draft 2020-12): objects, arrays, local $ref to $defs, enum/const, formats and
   the usual bounds. */

export interface JsonSchema {
  $ref?: string;
  $defs?: Record<string, JsonSchema>;
  definitions?: Record<string, JsonSchema>;
  title?: string;
  description?: string;
  type?: string | string[];
  properties?: Record<string, JsonSchema>;
  required?: string[];
  items?: JsonSchema;
  enum?: unknown[];
  const?: unknown;
  format?: string;
  pattern?: string;
  minimum?: number;
  maximum?: number;
  minLength?: number;
  maxLength?: number;
  default?: unknown;
  additionalProperties?: boolean | JsonSchema;
}

export interface SchemaRow {
  /** Dotted path; `[]` marks the items of an array (`lines[].amount`). */
  path: string;
  depth: number;
  type: string;
  required: boolean;
  description?: string;
  /** enum, const, format, pattern, bounds, default — already formatted. */
  constraints: string[];
}

function resolve(schema: JsonSchema, root: JsonSchema): JsonSchema {
  if (!schema.$ref) return schema;
  const match = /^#\/(\$defs|definitions)\/(.+)$/.exec(schema.$ref);
  const target = match ? root[match[1] as "$defs" | "definitions"]?.[match[2]] : undefined;
  if (!target) return schema;
  // Sibling keywords (a description next to the $ref) win over the target's.
  const siblings: JsonSchema = { ...schema };
  delete siblings.$ref;
  return { ...resolve(target, root), ...siblings };
}

function typeOf(schema: JsonSchema): string {
  if (schema.const !== undefined) return typeof schema.const === "string" ? "string" : typeof schema.const;
  const type = Array.isArray(schema.type) ? schema.type.join(" | ") : schema.type;
  if (type === "array" && schema.items) return `${typeOf(schema.items)}[]`;
  if (type) return type;
  if (schema.enum) return "enum";
  if (schema.properties) return "object";
  return "any";
}

function constraintsOf(schema: JsonSchema): string[] {
  const out: string[] = [];
  const show = (v: unknown) => JSON.stringify(v);
  if (schema.const !== undefined) out.push(`= ${show(schema.const)}`);
  if (schema.enum) out.push(schema.enum.map(show).join(" · "));
  if (schema.format) out.push(schema.format);
  if (schema.pattern) out.push(`/${schema.pattern}/`);
  if (schema.minimum !== undefined) out.push(`≥ ${schema.minimum}`);
  if (schema.maximum !== undefined) out.push(`≤ ${schema.maximum}`);
  if (schema.minLength !== undefined) out.push(`length ≥ ${schema.minLength}`);
  if (schema.maxLength !== undefined) out.push(`length ≤ ${schema.maxLength}`);
  if (schema.default !== undefined) out.push(`default ${show(schema.default)}`);
  return out;
}

/** One row per property, depth-first, nested objects and array items included. */
export function schemaRows(schema: JsonSchema, root: JsonSchema = schema, prefix = "", depth = 0): SchemaRow[] {
  const node = resolve(schema, root);
  const required = new Set(node.required ?? []);
  const rows: SchemaRow[] = [];
  for (const [name, raw] of Object.entries(node.properties ?? {})) {
    const property = resolve(raw, root);
    const path = prefix ? `${prefix}.${name}` : name;
    rows.push({
      path,
      depth,
      type: typeOf(property),
      required: required.has(name),
      description: property.description ?? property.title,
      constraints: constraintsOf(property),
    });
    if (property.properties) rows.push(...schemaRows(property, root, path, depth + 1));
    const items = property.items ? resolve(property.items, root) : undefined;
    if (items?.properties) rows.push(...schemaRows(items, root, `${path}[]`, depth + 1));
  }
  return rows;
}
