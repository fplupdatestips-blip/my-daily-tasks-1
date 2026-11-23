// Builds football.json: FPL gameweek deadlines plus every match an English
// Premier League team plays (league, cups, Europe, friendlies). Run by the
// Build workflow every few hours; the file is published with the web app at
// https://daily-tasks-19f9e.web.app/football.json and read by web + Android.
//
//   node scripts/football-feed.mjs web/public/football.json
//
// Sources (both free, no key):
//   - FPL: https://fantasy.premierleague.com/api/bootstrap-static/ (events[].deadline_time)
//   - ESPN: soccer/eng.1/teams for the current PL clubs, then
//     soccer/all/teams/{id}/schedule (results and fixtures, all competitions).
// If a source fails, that part is copied from the file that is live now, so
// a bad run never empties the calendar.

import { writeFile } from 'node:fs/promises';

const OUT = process.argv[2] ?? 'web/public/football.json';
const LIVE = 'https://daily-tasks-19f9e.web.app/football.json';
const ESPN = 'https://site.api.espn.com/apis/site/v2/sports/soccer';

async function getJson(url) {
  for (let attempt = 1; ; attempt++) {
    try {
      const r = await fetch(url, { headers: { 'user-agent': 'daily-tasks-feed/1.0', accept: 'application/json' } });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      return await r.json();
    } catch (e) {
      if (attempt >= 3) throw new Error(`${url}: ${e.message}`);
      await new Promise((res) => setTimeout(res, 1500 * attempt));
    }
  }
}

async function deadlines() {
  const j = await getJson('https://fantasy.premierleague.com/api/bootstrap-static/');
  const out = j.events
    .filter((e) => e.deadline_time)
    .map((e) => ({ gw: e.id, name: e.name, deadline: new Date(e.deadline_time).toISOString() }));
  if (!out.length) throw new Error('FPL returned no gameweeks');
  return out;
}

function toMatch(e) {
  const c = e.competitions?.[0];
  if (!c) return null;
  const side = (ha) => {
    const t = c.competitors.find((x) => x.homeAway === ha)?.team ?? {};
    return { id: String(t.id ?? ''), name: t.displayName ?? '?', short: t.shortDisplayName ?? t.displayName ?? '?', abbr: t.abbreviation ?? '' };
  };
  const status = c.status?.type ?? e.status?.type ?? {};
  return {
    id: String(e.id),
    kickoff: new Date(e.date).toISOString(),
    /** false when the kick-off time is not fixed yet (only the date is). */
    timeValid: c.timeValid !== false && e.timeValid !== false,
    home: side('home'),
    away: side('away'),
    competition: e.league?.name ?? e.season?.displayName ?? '',
    venue: c.venue?.fullName ?? '',
    /** pre | in | post; STATUS_POSTPONED etc. in statusName. */
    state: status.state ?? 'pre',
    statusName: status.name ?? '',
  };
}

async function matches() {
  const t = await getJson(`${ESPN}/eng.1/teams`);
  const teams = t.sports[0].leagues[0].teams.map((x) => ({ id: String(x.team.id), name: x.team.displayName }));
  if (teams.length < 18) throw new Error(`ESPN returned ${teams.length} PL teams`);
  const byId = new Map();
  let failures = 0;
  for (const team of teams) {
    for (const q of ['', '?fixture=true']) {
      try {
        const s = await getJson(`${ESPN}/all/teams/${team.id}/schedule${q}`);
        for (const e of s.events ?? []) {
          const m = toMatch(e);
          if (m) byId.set(m.id, m);
        }
      } catch (err) {
        failures++;
        console.warn(String(err));
      }
    }
  }
  if (failures > 4) throw new Error(`${failures} ESPN schedule requests failed`);
  const list = [...byId.values()]
    // Cancelled or postponed games have no real date to show.
    .filter((m) => !/POSTPONED|CANCELED|CANCELLED|ABANDONED/.test(m.statusName))
    .sort((a, b) => (a.kickoff < b.kickoff ? -1 : a.kickoff > b.kickoff ? 1 : 0));
  return { teams, list };
}

async function main() {
  let live = null;
  try {
    live = await getJson(LIVE);
  } catch {
    /* first run, or offline */
  }
  const out = { version: 1, updatedAt: new Date().toISOString(), plTeams: [], deadlines: [], matches: [] };
  try {
    out.deadlines = await deadlines();
  } catch (e) {
    console.warn(`FPL deadlines failed, keeping the live copy: ${e.message}`);
    out.deadlines = live?.deadlines ?? [];
  }
  try {
    const { teams, list } = await matches();
    out.plTeams = teams;
    out.matches = list;
  } catch (e) {
    console.warn(`Matches failed, keeping the live copy: ${e.message}`);
    out.plTeams = live?.plTeams ?? [];
    out.matches = live?.matches ?? [];
  }
  await writeFile(OUT, JSON.stringify(out));
  console.log(`Wrote ${OUT}: ${out.deadlines.length} deadlines, ${out.matches.length} matches`);
}

await main();
