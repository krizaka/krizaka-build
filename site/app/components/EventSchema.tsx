import { getDictionary, type Locale } from "@/lib/i18n";
import { schemaRows, type JsonSchema } from "@/lib/json-schema";

/* A JSON Schema (an event contract, an envelope) as a table: one row per field, nested fields indented. */
export function EventSchema({ schema, locale }: { schema: JsonSchema; locale: Locale }) {
  const t = getDictionary(locale).schema;
  const rows = schemaRows(schema);
  return (
    <div className="not-prose kz-table-scroll kz-schema">
      {schema.title && <p className="kz-props-component">{schema.title}</p>}
      <table className="kz-table">
        <thead>
          <tr>
            <th scope="col">{t.field}</th>
            <th scope="col">{t.type}</th>
            <th scope="col">{t.required}</th>
            <th scope="col">{t.description}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.path}>
              <th scope="row" style={{ paddingInlineStart: `calc(0.75rem + ${row.depth} * 1rem)` }}>
                <code>{row.path}</code>
              </th>
              <td><code className="kz-type">{row.type}</code></td>
              <td>{row.required ? t.yes : t.no}</td>
              <td>
                {row.description}
                {row.constraints.length > 0 && (
                  <span className="kz-constraints">
                    {row.constraints.map((c) => <code key={c}>{c}</code>)}
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
