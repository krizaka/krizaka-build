import { getDictionary, type Locale } from "@/lib/i18n";
import { configurationModules, formatDefault, shortType } from "@/lib/configuration";

/* Every krizaka.* property of an artifact (or of all of them), generated from the configuration metadata: name, type,
   default, description and, for a closed set, its values. Server Component. */
export function ConfigurationReference({ locale, artifact }: { locale: Locale; artifact?: string }) {
  const t = getDictionary(locale).configuration;
  const modules = configurationModules().filter((m) => !artifact || m.artifact === artifact);
  return (
    <div className="not-prose">
      {modules.map((m) => (
        <section key={m.artifact} aria-labelledby={`config-${m.artifact}`} style={{ margin: "20px 0 28px" }}>
          {!artifact && (
            <h3 id={`config-${m.artifact}`} className="kz-props-component">
              <code>{m.artifact}</code> · <a href={m.repositoryUrl}>{m.repository}</a>
            </h3>
          )}
          <div className="kz-table-scroll">
            <table className="kz-table kz-config">
              <thead>
                <tr>
                  <th scope="col">{t.property}</th>
                  <th scope="col">{t.description}</th>
                </tr>
              </thead>
              <tbody>
                {m.properties.map((p) => {
                  const values = m.hints.find((h) => h.name === p.name)?.values ?? [];
                  const fallback = formatDefault(p.defaultValue);
                  return (
                    <tr key={p.name} id={p.name}>
                      <th scope="row" className="kz-config-name">
                        <code>{p.name}</code>
                        <span className="kz-config-facts">
                          <code className="kz-type">{shortType(p.type)}</code>
                          {fallback === null ? <span>{t.noDefault}</span> : <span>{t.default} <code>{fallback}</code></span>}
                        </span>
                      </th>
                      <td>
                        {p.description}
                        {values.length > 0 && (
                          <span className="kz-constraints">
                            {values.map((v) => <code key={v.value} title={v.description}>{v.value}</code>)}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>
      ))}
    </div>
  );
}
