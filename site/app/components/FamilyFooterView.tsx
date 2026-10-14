"use client";

import { SiteFooter as FamilyFooter } from "@krizaka/ui/site-footer";
import { familyLabels } from "@krizaka/ui/sites";
import type { Locale } from "@/lib/i18n";

/* The client half of the footer: @krizaka/ui's SiteFooter.Column / .Link are members of a client component, so they are
   composed here; SiteFooter (server) reads the words and the BOM version at build and hands them in. */
export function FamilyFooterView({
  locale,
  aria,
  version,
  license,
  links,
}: {
  locale: Locale;
  aria: string;
  version?: string;
  license: string;
  links: { label: string; href: string }[];
}) {
  return (
    <FamilyFooter current="java" labels={familyLabels[locale]} aria-label={aria} version={version} legal={<span>{license}</span>}>
      <FamilyFooter.Column title="Krizaka Java">
        {links.map((l) => (
          <FamilyFooter.Link key={l.href} href={l.href}>
            {l.label}
          </FamilyFooter.Link>
        ))}
      </FamilyFooter.Column>
    </FamilyFooter>
  );
}
