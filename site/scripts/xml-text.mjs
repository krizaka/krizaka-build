/** The text of an XML node: character references decoded (`&amp;` last, so `&amp;lt;` reads `&lt;`, not `<`),
 *  whitespace collapsed. */
export const xmlText = (s) =>
  String(s).replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&amp;/g, "&").replace(/\s+/g, " ").trim();
