import type { CSSProperties, ReactNode } from "react";
import { DocsLayout } from "fumadocs-ui/layouts/docs";
import { ThemeProvider, ThemeScript } from "@krizaka/ui/theme";
import { SkipLink } from "@krizaka/ui/skip-link";
import { cn } from "@krizaka/ui/cn";
import { getDictionary, localizedPath, type Locale } from "@/lib/i18n";
import { treeOf } from "@/lib/source";
import { displayFace, monoFace, textFace } from "../fonts";
import { DocsProviders } from "./DocsProviders";
import { I18nProvider } from "./I18nProvider";
import { mavenVersion } from "@/lib/versions";
import SiteFooter from "./SiteFooter";
import { ThemeSync } from "./SiteTheme";
import TopBar from "./TopBar";
import "../globals.css";

/* The root of one language (the two root layouts, app/(en) and app/fr, are this): the document, the Krizaka faces, the
   theme (dark by default, persisted as `kz-theme`, no flash), the bar, Fumadocs' sidebar and page, the footer. */
export default function SiteRoot({ locale, children }: { locale: Locale; children: ReactNode }) {
  const t = getDictionary(locale);
  return (
    <html lang={locale} className={cn(textFace.variable, displayFace.variable, monoFace.variable, "h-full dark")} data-scroll-behavior="smooth" suppressHydrationWarning>
      <head>
        <ThemeScript defaultMode="dark" />
        <meta name="theme-color" content="#0d0d0f" />
      </head>
      <body className="flex min-h-dvh flex-col antialiased">
        <ThemeProvider defaultMode="dark">
          <ThemeSync />
          <I18nProvider locale={locale} messages={t}>
            <DocsProviders>
              <SkipLink href="#nd-page">{t.bar.skip}</SkipLink>
              <TopBar version={mavenVersion("krizaka-bom")} />
              <DocsLayout
                tree={treeOf(locale)}
                nav={{ title: t.site.name, url: localizedPath(locale, "/") }}
                themeSwitch={{ enabled: false }}
                containerProps={{ className: "kz-docs", style: { "--fd-banner-height": "var(--kz-docs-top)" } as CSSProperties }}
              >
                {children}
              </DocsLayout>
              <SiteFooter locale={locale} />
            </DocsProviders>
          </I18nProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
