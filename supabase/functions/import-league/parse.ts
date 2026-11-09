// Reads a FIP calendar PDF (ADR 0011). The plain text glues the two team names together and puts dates in a block
// apart, so the lines are rebuilt from the text coordinates and read in order: a line with home, away and date starts
// a fixture; the FIP number (left, a bit lower) and the venue (the line below, maybe on the next page) follow it.
// From esm.sh, not npm: the npm package bundles to 29 MB (canvas binaries) and the deploy is refused with 413.
import { getDocument } from "https://esm.sh/pdfjs-dist@4.10.38/legacy/build/pdf.mjs";
// The worker imported here, not by pdfjs at runtime: a dynamic import may be missing from the deployed bundle.
import * as pdfjsWorker from "https://esm.sh/pdfjs-dist@4.10.38/legacy/build/pdf.worker.mjs";
(globalThis as { pdfjsWorker?: unknown }).pdfjsWorker = pdfjsWorker;

export type Leg = "FIRST" | "SECOND";
export interface PreviewFixture {
  number: string;
  round: number;
  leg: Leg;
  startsAt: string | null; // local Europe/Rome time, "2026-11-09T18:30"; null if postponed
  home: string;
  away: string;
  venue: string | null;
}
export interface Preview {
  name: string;
  teams: string[];
  fixtures: PreviewFixture[];
}

interface Item { page: number; x: number; y: number; s: string }

const ROUND = /^(\d+) Giornata - (andata|ritorno)/;
const NUMBER = /^\d{6}$/;
const DATE = /^(\d{2})\/(\d{2})\/(\d{2}) - (\d{2}):(\d{2})$/;
const PAGE = /^pag\. \d+ di \d+$/;
// shortcut: columns at fixed x as in the FIP layout of 2026; if it changes the preview shows it and the test fails.
const NUMBER_X = 80;
const AWAY_X = 250;
const DATE_X = 480;
const text = (items: Item[]) => items.map((i) => i.s.trim()).join(" ").trim();

async function lines(data: Uint8Array): Promise<Item[][]> {
  const doc = await getDocument({ data, useSystemFonts: true }).promise;
  const out: Item[][] = [];
  for (let p = 1; p <= doc.numPages; p++) {
    const items = (await (await doc.getPage(p)).getTextContent()).items
      .filter((i) => "str" in i && i.str.trim() !== "" && !PAGE.test(i.str.trim()))
      .map((i) => ({ page: p, x: (i as any).transform[4], y: (i as any).transform[5], s: (i as any).str as string }))
      .sort((a, b) => b.y - a.y || a.x - b.x);
    for (const it of items) {
      const last = out.at(-1);
      if (last && last[0].page === p && Math.abs(last[0].y - it.y) < 2) last.push(it);
      else out.push([it]);
    }
  }
  return out.map((l) => l.sort((a, b) => a.x - b.x));
}

export async function parseCalendar(data: Uint8Array): Promise<Preview> {
  let name = "";
  let round = 0;
  let leg: Leg = "FIRST";
  const fixtures: PreviewFixture[] = [];
  for (const line of await lines(data)) {
    const all = text(line);
    const r = all.match(ROUND);
    const current = fixtures.at(-1);
    if (r) {
      round = Number(r[1]);
      leg = r[2] === "andata" ? "FIRST" : "SECOND";
    } else if (line.some((i) => i.x >= AWAY_X && i.x < DATE_X)) {
      const date = text(line.filter((i) => i.x >= DATE_X)).match(DATE);
      fixtures.push({
        number: "",
        round,
        leg,
        startsAt: date ? `20${date[3]}-${date[2]}-${date[1]}T${date[4]}:${date[5]}` : null,
        home: text(line.filter((i) => i.x < AWAY_X)),
        away: text(line.filter((i) => i.x >= AWAY_X && i.x < DATE_X)),
        venue: null,
      });
    } else if (line.length === 1 && line[0].x < NUMBER_X && NUMBER.test(all)) {
      if (current && current.number === "") current.number = all;
    } else if (!current && name === "") {
      name = all;
    } else if (current && current.venue === null) {
      current.venue = all;
    }
  }
  const teams = [...new Set(fixtures.flatMap((f) => [f.home, f.away]))].sort();
  return { name, teams, fixtures };
}
