import { assertEquals } from "jsr:@std/assert@1.0.13";
import { parseCalendar } from "./parse.ts";

const pdf = await Deno.readFile(new URL("../../../docs/Calendario.pdf", import.meta.url));
const preview = await parseCalendar(pdf);

Deno.test("reads the league name from the title", () => {
  assertEquals(preview.name, "Under 19 Maschile Regionale - PRIMA FASE");
});

Deno.test("reads 110 fixtures and 11 teams", () => {
  assertEquals(preview.fixtures.length, 110);
  assertEquals(new Set(preview.fixtures.map((f) => f.number)).size, 110);
  assertEquals(preview.teams.length, 11);
  assertEquals(preview.teams.includes("ARTEMIS BASKET A.S.D."), true);
});

Deno.test("each fixture has its round, number, date and time, teams and venue", () => {
  assertEquals(preview.fixtures[0], {
    number: "000691", round: 1, leg: "FIRST", startsAt: "2026-11-09T18:30",
    home: "ASD Atessa Basket", away: "ARTEMIS BASKET A.S.D.",
    venue: "Palestra Scuola Media - Via D. Ciampoli ATESSA (CH)",
  });
  // The venue split in two pieces of text is joined back.
  assertEquals(preview.fixtures.find((f) => f.number === "000694")?.venue,
    "Palestra Sc. T. Patini - Via Francia - Loc. Pettino L'AQUILA (AQ)");
  // The row's own date, not the round's indicative one; across a page break.
  assertEquals(preview.fixtures.find((f) => f.number === "000714"),
    { number: "000714", round: 5, leg: "FIRST", startsAt: "2026-11-30T18:15",
      home: "A.S.D. VIRTUS BK MONTESILVANO", away: "A.S.D. VILLAGE BASKET",
      venue: "PALASENNA - Via Senna MONTESILVANO (PE)" });
  // A fixture split by a page break: number lower than usual, venue on the next page.
  assertEquals(preview.fixtures.find((f) => f.number === "000704"),
    { number: "000704", round: 3, leg: "FIRST", startsAt: "2026-11-23T20:30",
      home: "Teramo a Spicchi", away: "A.S.D. VILLAGE BASKET",
      venue: "PALASPORT ACQUAVIVA - VIA DEL TIRO TERAMO (TE)" });
  assertEquals(preview.fixtures.at(-1)?.round, 11);
  assertEquals(preview.fixtures.at(-1)?.leg, "SECOND");
});

Deno.test("every team plays 20 games, 10 at home", () => {
  for (const t of preview.teams) {
    assertEquals(preview.fixtures.filter((f) => f.home === t || f.away === t).length, 20, t);
    assertEquals(preview.fixtures.filter((f) => f.home === t).length, 10, t);
  }
});

Deno.test("each round has 5 fixtures", () => {
  for (const leg of ["FIRST", "SECOND"]) {
    for (let r = 1; r <= 11; r++) {
      assertEquals(preview.fixtures.filter((f) => f.leg === leg && f.round === r).length, 5, `${r} ${leg}`);
    }
  }
});
