import Link from "next/link";
import { CheckIcon, ExternalIcon, ForwardIcon, GlobeIcon, MessageIcon, PackIcon, SettingsIcon, ShieldIcon, EyeIcon, StudioIcon, BookmarkIcon, LinkIcon, ClockIcon } from "@krizaka/icons";
import { Button } from "@krizaka/ui/button";
import { ArgumentCard } from "@krizaka/ui/argument-card";
import { CardGrid } from "@krizaka/ui/card-grid";
import { CodeBlock, CodeTabs } from "@krizaka/ui/code-block";
import { ComparisonTable } from "@krizaka/ui/comparison-table";
import { LandingPage } from "@krizaka/ui/landing-page";
import { LinkCard } from "@krizaka/ui/link-card";
import { Mascot } from "@krizaka/ui/mascots";
import { MascotQuote } from "@krizaka/ui/mascot-quote";
import { NumberTicker } from "@krizaka/ui/number-ticker";
import { Section } from "@krizaka/ui/page";
import { format, getDictionary, localizedPath, type Locale } from "@/lib/i18n";
import type { FamilyId } from "@/lib/families";
import { counts, families, starterModules } from "@/lib/home";
import { proof, roundedDown } from "@/lib/proof";
import { MAVEN_CENTRAL_URL, REPOSITORY_URL, mavenArtifactUrl } from "@/lib/site";
import { mavenVersion } from "@/lib/versions";
import BomWindow from "./BomWindow";

/* The home of java.krizaka.com, on the landing template of @krizaka/ui: a split hero (the promise, and a pom.xml that
   writes the BOM import and lights up the starters), the facts read at build, why (three pillars), the families of
   building blocks (what each service no longer writes), the duck, the real snippet in Maven and Gradle, the proof (who
   runs on it, the tests), an honest comparison, the ways on. Every figure is data (Maven Central, the POMs, the
   repositories' tests); every word is a message. */

const ICONS: Record<FamilyId, typeof GlobeIcon> = { web: GlobeIcon, security: ShieldIcon, messaging: MessageIcon, observability: EyeIcon, appConfig: SettingsIcon, testing: CheckIcon };
const COMPARE_ROWS = ["errors", "security", "messaging", "observability", "freedom"] as const;
const UI_URL = "https://ui.krizaka.com";

export default function Home({ locale }: { locale: Locale }) {
  const dict = getDictionary(locale);
  const t = dict.home;
  const path = (p: string) => localizedPath(locale, p);
  const bom = mavenVersion("krizaka-bom");
  if (!bom) throw new Error("home: krizaka-bom is not on Maven Central — the home states only what is published");
  const { platform, tests, services } = proof();
  const { modules } = counts();
  const blocks = families();
  const published = starterModules();
  const shown = ["web", "observability"] as const;
  const starterId = (id: (typeof shown)[number]) => blocks.find((f) => f.id === id)?.starter ?? "";

  const maven = `<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.krizaka</groupId>
      <artifactId>krizaka-bom</artifactId>
      <version>${bom}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
${shown.map((id) => `  <dependency>\n    <groupId>com.krizaka</groupId>\n    <artifactId>${starterId(id)}</artifactId>\n  </dependency>`).join("\n")}
</dependencies>`;
  const gradle = `dependencies {
    implementation(platform("com.krizaka:krizaka-bom:${bom}"))
${shown.map((id) => `    implementation("com.krizaka:${starterId(id)}")`).join("\n")}
}`;
  const copy = { copyLabel: t.install.copy, copiedLabel: t.install.copied };

  return (
    <main id="nd-page" className="jh-home">
      <LandingPage
        eyebrow={t.hero.eyebrow}
        title={t.hero.title}
        lead={t.hero.lead}
        actions={
          <>
            <Button asChild variant="primary" size="lg" shape="cut" shimmer magnetic>
              <Link href={path("/start")}>
                {t.hero.start} <ForwardIcon size={16} aria-hidden />
              </Link>
            </Button>
            <Button asChild variant="secondary" size="lg" shape="cut">
              <Link href={path("/map")}>{t.hero.map}</Link>
            </Button>
          </>
        }
        visualLabel={t.window.label}
        visual={<BomWindow version={bom} starters={published} words={{ aria: t.window.aria, pulls: t.window.pulls, pullsHint: t.window.pullsHint }} />}
        proofs={[
          { value: <span className="jh-mono">{bom}</span>, label: t.proofs.bom },
          { value: <NumberTicker value={modules} locale={locale} />, label: t.proofs.modules },
          { value: platform.java, label: t.proofs.java },
          { value: platform.springBootMinor, label: t.proofs.boot },
          { value: "Apache-2.0", label: t.proofs.license },
        ]}
        featuresHeading={{ eyebrow: t.pillars.eyebrow, title: t.pillars.title, lead: t.pillars.lead }}
        features={[
          { id: "bom", size: "lg", icon: <PackIcon size={22} />, title: t.pillars.bom.title, text: t.pillars.bom.text },
          { id: "proven", size: "md", icon: <CheckIcon size={22} />, title: t.pillars.proven.title, text: t.pillars.proven.text },
          { id: "capabilities", size: "md", icon: <StudioIcon size={22} />, title: t.pillars.capabilities.title, text: t.pillars.capabilities.text },
        ]}
        closing={{
          title: t.closing.title,
          body: t.closing.body,
          actions: (
            <>
              <Button asChild variant="primary" size="lg" shape="cut">
                <Link href={path("/start")}>{t.closing.start}</Link>
              </Button>
              <Button asChild variant="secondary" size="lg" shape="cut">
                <a href={REPOSITORY_URL}>
                  {t.closing.source} <ExternalIcon size={16} aria-hidden />
                </a>
              </Button>
            </>
          ),
        }}
      >
        <Section id="families" eyebrow={t.families.eyebrow} title={t.families.title} lead={t.families.lead} reveal divider>
          <CardGrid min={290} columns={3}>
            {blocks.map((f) => {
              const Icon = ICONS[f.id];
              const words = t.families[f.id];
              return (
                <ArgumentCard
                  key={f.id}
                  id={`family-${f.id}`}
                  icon={<Icon size={22} aria-hidden />}
                  title={words.title}
                  meta={f.starter ?? f.module}
                  mark={f.bird ? <Mascot bird={f.bird} size={40} animated={false} /> : undefined}
                  hook={{ label: t.families.hookLabel, text: words.hook }}
                  facts={[
                    f.central
                      ? { label: format(t.families.published, { version: f.central }), tone: "ok", href: mavenArtifactUrl(f.module) }
                      : { label: t.families.unpublished, tone: "wip" },
                  ]}
                  more={{ href: path(f.guide), label: t.families.details }}
                  linkAs={Link}
                />
              );
            })}
          </CardGrid>
          <div className="jh-duck">
            <MascotQuote bird="duck" mood="welcome" variant="callout" label={t.quote.label} data-reveal="">
              {t.quote.text}
            </MascotQuote>
          </div>
        </Section>

        <Section id="install" eyebrow={t.install.eyebrow} title={t.install.title} lead={t.install.lead} reveal divider>
          <CodeTabs
            label={t.install.tabs}
            tabs={[
              { id: "maven", label: t.install.maven, content: <CodeBlock code={maven} language="xml" file="pom.xml" {...copy} /> },
              { id: "gradle", label: t.install.gradle, content: <CodeBlock code={gradle} language="kotlin" file="build.gradle.kts" {...copy} /> },
            ]}
          />
          <p className="jh-more">
            <Link href={path("/build")}>{t.install.more} →</Link>
          </p>
        </Section>

        <Section id="proof" eyebrow={t.proof.eyebrow} title={t.proof.title} lead={t.proof.lead} reveal divider>
          <div className="jh-proof">
            <dl className="jh-figures">
              <div>
                <dt>{t.proof.tests}</dt>
                <dd>
                  <NumberTicker value={roundedDown(tests.total)} locale={locale} />+
                </dd>
              </div>
              <div>
                <dt>{t.proof.real}</dt>
                <dd>
                  <NumberTicker value={roundedDown(tests.realInfrastructure, 10)} locale={locale} />+
                </dd>
              </div>
              <div>
                <dt>{t.proof.services}</dt>
                <dd>
                  <NumberTicker value={services.length} locale={locale} />
                </dd>
              </div>
            </dl>
            <ul className="jh-services" aria-label={t.proof.listLabel}>
              {services.map((s) => (
                <li key={s.id}>
                  <code>{s.id}</code>
                  <span>{s.artifacts.length === 1 ? t.proof.usesOne : format(t.proof.uses, { count: s.artifacts.length })}</span>
                </li>
              ))}
            </ul>
          </div>
          <p className="jh-more">
            {t.proof.domain} <Link href={path("/map")}>{t.proof.domainLink} →</Link>
          </p>
        </Section>

        <Section id="compare" eyebrow={t.compare.eyebrow} title={t.compare.title} lead={t.compare.lead} reveal divider>
          <ComparisonTable
            label={t.compare.label}
            topicLabel={t.compare.topic}
            highlight="krizaka"
            columns={[
              { id: "plain", label: t.compare.plain },
              { id: "krizaka", label: t.compare.krizaka },
            ]}
            rows={COMPARE_ROWS.map((id) => ({
              id,
              topic: t.compare.rows[id].topic,
              answers: { plain: t.compare.rows[id].plain, krizaka: t.compare.rows[id].krizaka },
            }))}
          />
        </Section>

        <Section id="go" eyebrow={t.go.eyebrow} title={t.go.title} reveal divider>
          <CardGrid min={260} columns={3}>
            <LinkCard href={path("/start")} linkAs={Link} icon={<StudioIcon size={22} aria-hidden />} title={t.go.guides.title} description={t.go.guides.text} />
            <LinkCard href={path("/map")} linkAs={Link} icon={<LinkIcon size={22} aria-hidden />} title={t.go.map.title} description={t.go.map.text} />
            <LinkCard href={path("/changelog")} linkAs={Link} icon={<ClockIcon size={22} aria-hidden />} title={t.go.changelog.title} description={t.go.changelog.text} />
            <LinkCard href={REPOSITORY_URL} icon={<BookmarkIcon size={22} aria-hidden />} title={t.go.source.title} description={t.go.source.text} />
            <LinkCard href={MAVEN_CENTRAL_URL} icon={<PackIcon size={22} aria-hidden />} title={t.go.maven.title} description={t.go.maven.text} />
            <LinkCard href={UI_URL} icon={<GlobeIcon size={22} aria-hidden />} title={t.go.ui.title} description={t.go.ui.text} />
          </CardGrid>
        </Section>
      </LandingPage>
    </main>
  );
}
