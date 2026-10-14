/* Canonical URLs, alternates, Open Graph and JSON-LD of a page. Every page is canonical to itself in its own language and
   names the other one (hreflang); the root of English is also x-default. */

import type { Metadata } from "next";
import { getDictionary, localizedPath, LOCALES, type Locale } from "./i18n";
import { SITE_URL } from "./site";

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
      type: "article",
      siteName: t.site.name,
      title: fullTitle,
      description,
      url,
      locale: locale === "en" ? "en_US" : "fr_CA",
      alternateLocale: locale === "en" ? "fr_CA" : "en_US",
    },
    twitter: { card: "summary_large_image", title: fullTitle, description },
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
      isPartOf: { "@type": "WebSite", name: t.site.name, url: SITE_URL },
      publisher: { "@type": "Organization", name: "Krizaka", url: "https://www.krizaka.com" },
    },
  ];
}
