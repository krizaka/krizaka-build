import test from "node:test";
import assert from "node:assert/strict";
import { xmlText } from "../scripts/xml-text.mjs";

test("xmlText decodes each reference once", () => {
  assert.equal(xmlText("a &lt;b&gt; &amp; c"), "a <b> & c");
  assert.equal(xmlText("&amp;lt;script&amp;gt;"), "&lt;script&gt;");
  assert.equal(xmlText("  spaced \n out "), "spaced out");
});
