import { getDictionary, type Locale } from "@/lib/i18n";
import { MAVEN_CENTRAL_URL, REPOSITORY_URL } from "@/lib/site";
import { mavenVersion } from "@/lib/versions";
import { FamilyFooterView } from "./FamilyFooterView";

/* The footer (NAV-SYS) is @krizaka/ui's SiteFooter: this site's own column (where the artifacts and the code are), the
   Krizaka family in columns, the signature, the release the pages were built from (the BOM on Maven Central). */
export default function SiteFooter({ locale }: { locale: Locale }) {
  const t = getDictionary(locale).footer;
  return (
    <FamilyFooterView
      locale={locale}
      aria={t.aria}
      version={mavenVersion("krizaka-bom") ?? undefined}
      license={t.license}
      links={[
        { label: t.maven, href: MAVEN_CENTRAL_URL },
        { label: t.source, href: REPOSITORY_URL },
      ]}
    />
  );
}
