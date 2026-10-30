import { buildInfo } from "../../scripts/build-info.mjs";

/* The commit of this build (APP_COMMIT, bound by App Platform at build time): the post-deploy workflow waits for it. */
export const dynamic = "force-static";

export function GET() {
  return Response.json(buildInfo(process.env.APP_COMMIT));
}
