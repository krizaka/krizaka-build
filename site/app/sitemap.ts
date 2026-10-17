import type { MetadataRoute } from "next";
import { LOCALES } from "@/lib/i18n";
import { absoluteUrl, pagePath } from "@/lib/seo";
import { source } from "@/lib/source";

const CHANGELOG = "/changelog";

export const dynamic = "force-static";

/* Every page in both languages, each naming the other (hreflang), dated by the build. */
export default function sitemap(): MetadataRoute.Sitemap {
  const lastModified = new Date();
  const paths = [...source.getPages("en").map((page) => pagePath(page.slugs)), CHANGELOG];
  return paths.flatMap((path) => {
    const languages = Object.fromEntries(LOCALES.map((l) => [l, absoluteUrl(l, path)]));
    return LOCALES.map((l) => ({ url: absoluteUrl(l, path), lastModified, alternates: { languages } }));
  });
}
