/* Canonical URLs, alternates, Open Graph and JSON-LD of a page. Every page is canonical to itself in its own language and
   names the other one (hreflang); the root of English is also x-default. */

import type { Metadata } from "next";
import { getDictionary, localizedPath, LOCALES, type Locale } from "./i18n";
import { KRIZAKA_URL, REPOSITORY_URL, SITE_URL } from "./site";

/** The Krizaka Organization, defined by www.krizaka.com: every JSON-LD node of the family points back to it. */
export const ORG_ID = `${KRIZAKA_URL}/#organization`;
const ORG = { "@type": "Organization", "@id": ORG_ID, name: "Krizaka", url: KRIZAKA_URL };
/** One social image for every page (app/og.png/route.tsx). */
export const OG_IMAGE = `${SITE_URL}/og.png`;

export const absoluteUrl = (locale: Locale, path: string) => `${SITE_URL}${localizedPath(locale, path)}`;

export const pagePath = (slugs: string[]) => (slugs.length ? `/${slugs.join("/")}` : "/");

export function pageMetadata(locale: Locale, path: string, title: string, description: string): Metadata {
  const t = getDictionary(locale);
  const fullTitle = path === "/" ? title : `${title} — ${t.site.name}`;
  const url = absoluteUrl(locale, path);
  return {
    title: { absolute: fullTitle },
    description,
    metadataBase: new URL(SITE_URL),
    alternates: {
      canonical: url,
      languages: { ...Object.fromEntries(LOCALES.map((l) => [l, absoluteUrl(l, path)])), "x-default": absoluteUrl("en", path) },
    },
    openGraph: {
      type: path === "/" ? "website" : "article",
      siteName: t.site.name,
      title: fullTitle,
      description,
      url,
      locale: locale === "en" ? "en_US" : "fr_CA",
      alternateLocale: locale === "en" ? "fr_CA" : "en_US",
      images: [{ url: OG_IMAGE, width: 1200, height: 630, alt: t.site.name }],
    },
    twitter: { card: "summary_large_image", title: fullTitle, description, images: [OG_IMAGE] },
    icons: { icon: "/favicon.svg" },
  };
}

export function articleJsonLd(locale: Locale, path: string, title: string, description: string) {
  const t = getDictionary(locale);
  const url = absoluteUrl(locale, path);
  const crumbs = [{ name: t.site.name, url: absoluteUrl(locale, "/") }, ...(path === "/" ? [] : [{ name: title, url }])];
  return [
    {
      "@context": "https://schema.org",
      "@type": "BreadcrumbList",
      itemListElement: crumbs.map((c, i) => ({ "@type": "ListItem", position: i + 1, name: c.name, item: c.url })),
    },
    {
      "@context": "https://schema.org",
      "@type": "TechArticle",
      headline: title,
      description,
      url,
      inLanguage: locale,
      isPartOf: { "@id": `${SITE_URL}/#website` },
      author: { "@id": ORG_ID },
      publisher: ORG,
    },
    ...(path === "/" ? siteJsonLd(locale) : []),
  ];
}

/** The home: the WebSite and the source code it documents (Krizaka Java), both published by the Krizaka Organization.
    No SearchAction: the search is a dialog (⌘K) over a static index, there is no search URL to declare. */
function siteJsonLd(locale: Locale) {
  const t = getDictionary(locale);
  return [
    {
      "@context": "https://schema.org",
      "@type": "WebSite",
      "@id": `${SITE_URL}/#website`,
      url: SITE_URL,
      name: t.site.name,
      inLanguage: ["en", "fr"],
      publisher: ORG,
    },
    {
      "@context": "https://schema.org",
      "@type": "SoftwareSourceCode",
      "@id": `${SITE_URL}/#source`,
      name: t.site.name,
      url: SITE_URL,
      description: t.site.description,
      codeRepository: REPOSITORY_URL,
      programmingLanguage: "Java",
      runtimePlatform: "Java 21, Spring Boot 4",
      license: "https://www.apache.org/licenses/LICENSE-2.0",
      isAccessibleForFree: true,
      publisher: ORG,
      isPartOf: { "@id": `${SITE_URL}/#website` },
    },
  ];
}
