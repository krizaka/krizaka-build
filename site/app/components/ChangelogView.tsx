import { CHANGELOG_REPO, loadReleases } from "@/lib/changelog";
import { getDictionary, type Locale, localizedPath } from "@/lib/i18n";
import { uiVersion } from "@/lib/platform";
import { mavenVersion } from "@/lib/versions";
import { ChangelogBody } from "./ChangelogBody";

/** `/changelog/` and `/fr/changelog/` (changelog template), inside the docs layout: the releases of Krizaka Java, read from GitHub at build. */
export async function ChangelogView({ locale }: { locale: Locale }) {
  const t = getDictionary(locale);
  return (
    <ChangelogBody
      c={t.changelog}
      releases={await loadReleases()}
      version={mavenVersion("krizaka-bom") ?? ""}
      ui={uiVersion()}
      back={{ href: localizedPath(locale, "/"), label: t.site.name }}
      feedHref={`${localizedPath(locale, "/changelog")}feed.xml`}
      pullsUrl={`https://github.com/${CHANGELOG_REPO}/releases`}
    />
  );
}
