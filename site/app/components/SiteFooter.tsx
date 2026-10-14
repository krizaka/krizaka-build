import { format, getDictionary, type Locale } from "@/lib/i18n";
import { GITHUB_ORG_URL, KRIZAKA_URL, MAVEN_CENTRAL_URL, REPOSITORY_URL, UI_URL } from "@/lib/site";
import { mavenVersion } from "@/lib/versions";

/* The footer: whose project this is, where the code and the artifacts are, the release the pages were built from. */
export default function SiteFooter({ locale }: { locale: Locale }) {
  const t = getDictionary(locale).footer;
  const bom = mavenVersion("krizaka-bom");
  return (
    <footer className="kz-footer">
      <div>
        <p><a href={KRIZAKA_URL}>{t.project}</a> · {t.license}</p>
        <p>{t.built}{bom ? ` ${format(t.version, { version: bom })}` : ""}</p>
      </div>
      <nav aria-label={t.aria}>
        <a href={KRIZAKA_URL}>{t.krizaka}</a>
        <a href={UI_URL}>{t.ui}</a>
        <a href={MAVEN_CENTRAL_URL}>{t.maven}</a>
        <a href={REPOSITORY_URL}>{t.source}</a>
        <a href={GITHUB_ORG_URL}>GitHub</a>
      </nav>
    </footer>
  );
}
