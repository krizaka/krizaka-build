import { highlight } from "@krizaka/ui/code-block";
import { format } from "@/lib/i18n";

/* The hero's visual: a pom.xml that writes the six lines of the BOM import (each line typed in turn), then lights up the
   starters it makes available, one dependency each, with the module each switches on. Pure CSS (globals.css, « The home »):
   the server renders the finished state — what a reader sees without the motion (reduced motion, no script) is the
   whole window — and the animation only plays it from the start. The version, the starters and their modules are data. */

export interface BomWindowProps {
  version: string;
  /** The published starters and the library module each one pulls. */
  starters: { starter: string; module: string }[];
  words: { aria: string; pulls: string; pullsHint: string };
}

const bomLines = (version: string) => [
  "<dependency>",
  "  <groupId>com.krizaka</groupId>",
  "  <artifactId>krizaka-bom</artifactId>",
  `  <version>${version}</version>`,
  "  <type>pom</type> <scope>import</scope>",
  "</dependency>",
];

export default function BomWindow({ version, starters, words }: BomWindowProps) {
  const lines = bomLines(version);
  return (
    <div className="jh-window" role="group" aria-label={words.aria}>
      <div className="kz-code jh-code">
        <pre>
          <code>
            {lines.map((text, i) => (
              <span key={i} className="jh-line" style={{ "--i": i, "--ch": text.length } as React.CSSProperties}>
                {highlight(text, "xml")}
                {"\n"}
              </span>
            ))}
          </code>
        </pre>
      </div>
      <p className="jh-pulls">
        <strong>{format(words.pulls, { count: starters.length })}</strong>
        <span>{words.pullsHint}</span>
      </p>
      <ul className="jh-lit">
        {starters.map((s, i) => (
          <li key={s.starter} style={{ "--k": i } as React.CSSProperties}>
            <code className="jh-starter">{s.starter}</code>
            <span aria-hidden className="jh-arrow">→</span>
            <code className="jh-module">{s.module}</code>
          </li>
        ))}
      </ul>
    </div>
  );
}
