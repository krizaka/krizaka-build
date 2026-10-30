/* The build id served at /build.json: the commit the site was built from. The post-deploy workflow (krizaka/.github
   post-deploy.yml) polls the live site for it, so IndexNow and the SEO guard run only once the new build is served.
   DigitalOcean binds APP_COMMIT to `${_self.COMMIT_HASH}` at build time (deploy/app.yaml); a local build says "local".
   Pure and tested (tests/build-info.test.mjs); read by app/build.json/route.ts. */
export function buildInfo(commit) {
  const value = String(commit ?? "").trim();
  return { commit: /^[0-9a-f]{40}$/i.test(value) ? value.toLowerCase() : "local" };
}
