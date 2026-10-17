import type { Metadata } from "next";
import { ChangelogView } from "@/app/components/ChangelogView";
import { getDictionary } from "@/lib/i18n";
import { pageMetadata } from "@/lib/seo";

export function generateMetadata(): Metadata {
  const c = getDictionary("en").changelog;
  return pageMetadata("en", "/changelog", c.metaTitle, c.metaDescription);
}

export default function Page() {
  return <ChangelogView locale="en" />;
}
