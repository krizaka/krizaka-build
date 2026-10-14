"use client";

/* The bar (NAV-SYS), built on @krizaka/ui's NavBar (floating, tightening on scroll, one light gliding between the items).
   Left: the mark, the family (NavBar.Family, @krizaka/ui/sites) and the BOM version (NavBar.Badge). Centre, this site:
   Get started · Building blocks · Guides — never a link to another site. Right: search · Repo · theme · language.
   Below md: the mark, the search and the mobile panel's toggle (the family is its last group). */

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSearchContext } from "fumadocs-ui/contexts/search";
import { GlobeIcon, MoonIcon, SearchIcon, SunIcon } from "@krizaka/icons";
import { KrizakaLogo } from "@krizaka/ui";
import { Button } from "@krizaka/ui/button";
import { Kbd } from "@krizaka/ui/kbd";
import { NavBar } from "@krizaka/ui/nav-bar";
import { familyLabels } from "@krizaka/ui/sites";
import { localizedPath, splitLocale } from "@/lib/i18n";
import { REPOSITORY_URL } from "@/lib/site";
import { useI18n } from "./I18nProvider";
import { useSiteTheme } from "./SiteTheme";

export default function TopBar({ version }: { version: string | null }) {
  const { locale, t } = useI18n();
  const b = t.bar;
  const { theme, toggleTheme } = useSiteTheme();
  const { setOpenSearch } = useSearchContext();
  const { path } = splitLocale(usePathname() ?? "/");
  const clean = path === "/" ? "/" : path.replace(/\/$/, "");
  const other = locale === "en" ? "fr" : "en";
  const home = localizedPath(locale, "/");
  const map = localizedPath(locale, "/map");
  const guides = localizedPath(locale, "/web");

  return (
    <div className="kz-site-bar-wrap pointer-events-none fixed inset-x-0 top-3 z-50">
      <NavBar.Root label={b.aria} className="kz-site-bar pointer-events-auto">
        <NavBar.Brand asChild>
          <Link href={home}>
            <KrizakaLogo size={26} />
            {t.site.name}
          </Link>
        </NavBar.Brand>
        <NavBar.Family current="java" labels={familyLabels[locale]} />
        {version && <NavBar.Badge version={version} title={b.versionAria} />}

        <NavBar.Items>
          <NavBar.Link asChild active={clean === "/"}>
            <Link href={home}>{b.start}</Link>
          </NavBar.Link>
          <NavBar.Link asChild active={clean === "/map"}>
            <Link href={map}>{b.map}</Link>
          </NavBar.Link>
          <NavBar.Link asChild active={clean !== "/" && clean !== "/map"}>
            <Link href={guides}>{b.guides}</Link>
          </NavBar.Link>
        </NavBar.Items>

        <NavBar.Actions className="gap-1">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            shape="pill"
            onClick={() => setOpenSearch(true)}
            aria-label={b.searchAria}
            aria-keyshortcuts="Meta+K Control+K"
            className="w-9 px-0 font-normal text-fg-secondary lg:w-auto lg:pl-3 lg:pr-1.5"
          >
            <SearchIcon size={14} />
            <span className="hidden lg:inline">{b.search}</span>
            <Kbd className="hidden lg:inline-flex">⌘K</Kbd>
          </Button>

          <span aria-hidden className="mx-1 hidden h-4 w-px bg-border-subtle lg:block" />

          <NavBar.Repo href={REPOSITORY_URL} label={b.githubAria} className="hidden lg:inline-flex" />

          <Button type="button" variant="ghost" size="icon" shape="pill" onClick={toggleTheme} aria-label={theme === "dark" ? b.toLight : b.toDark} className="hidden h-9 w-9 md:inline-flex">
            {theme === "dark" ? <SunIcon size={16} /> : <MoonIcon size={16} />}
          </Button>

          <Button asChild variant="outline" size="sm" className="hidden h-7 w-9 px-0 font-mono text-[11px] font-semibold tracking-wide md:inline-flex">
            <Link href={localizedPath(other, clean)} hrefLang={other} lang={other} aria-label={b.switchLanguage}>
              {b.languageShort}
            </Link>
          </Button>

          <NavBar.Mobile openLabel={b.menu} closeLabel={b.menu} className="max-h-[calc(100dvh-6rem)] gap-1 overflow-y-auto overscroll-contain p-3">
            <MobileLink href={home}>{b.start}</MobileLink>
            <MobileLink href={map}>{b.map}</MobileLink>
            <MobileLink href={guides}>{b.guides}</MobileLink>
            <div className="mt-2 flex gap-2 border-t border-border-subtle pt-3">
              <Button type="button" variant="secondary" size="md" onClick={toggleTheme} className="min-h-11 flex-1">
                {theme === "dark" ? <SunIcon size={16} /> : <MoonIcon size={16} />}
                {theme === "dark" ? b.toLight : b.toDark}
              </Button>
              <Button asChild variant="secondary" size="md" className="min-h-11">
                <Link href={localizedPath(other, clean)} hrefLang={other} lang={other}>
                  <GlobeIcon size={16} />
                  {b.switchLanguage}
                </Link>
              </Button>
            </div>
            <NavBar.Family current="java" labels={familyLabels[locale]} />
          </NavBar.Mobile>
        </NavBar.Actions>
      </NavBar.Root>
    </div>
  );
}

function MobileLink({ href, children }: { href: string; children: string }) {
  const cls = "flex min-h-11 items-center rounded-xl px-3 font-display text-[15px] font-semibold text-fg no-underline hover:bg-surface-2";
  return <Link href={href} className={cls}>{children}</Link>;
}
