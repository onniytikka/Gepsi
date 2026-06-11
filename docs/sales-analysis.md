# Using Gepsi data for door-to-door sales analysis with Claude

Gepsi already captures three things that matter for sales analysis: **where** you were
(track points), **when** you were there (timestamps on every point and note), and
**what happened** (text notes and voice memos dropped at specific doors). This document
covers what Claude can do with that data today, what additional data would make the
analysis dramatically better, and a concrete pipeline for running the analysis.

## What Claude can do with the data Gepsi collects today

### 1. Voice memos → transcripts → structured outcomes

Claude's API does not accept audio directly, so voice memos need a speech-to-text step
first (OpenAI Whisper — open source, runs locally, handles Finnish and English well —
or Google Speech-to-Text). Once transcribed, Claude is very good at turning free-form
field notes like *"talked to an older couple, they already have a contract until spring
but asked me to come back in May"* into structured records:

```json
{ "outcome": "callback", "objection": "existing contract", "callbackHint": "May",
  "sentiment": "positive", "product_interest": null }
```

Run over a week of memos, this gives you an outcome table you never had to type.

### 2. Geographic + temporal pattern analysis

Because every note has `lat`, `lon`, and `ts`, and every route has a full GPS trace,
Claude can compute or reason about:

- **Conversion by area** — which streets/neighborhoods produce sales vs. rejections.
- **Time-of-day and day-of-week effects** — when do people answer the door, when do
  they buy. Note timestamps give you "doors knocked per hour" for free.
- **Route efficiency** — distance walked vs. doors logged vs. outcomes; spotting
  zigzagging or re-covered ground by comparing track polylines across days.
- **Coverage maps** — which areas of a territory are untouched (gaps between route
  polylines).

For numerically heavy parts (clustering, per-area rates), the best pattern is to have
Claude *write the analysis code* (Python + pandas over the exported JSON) rather than
do arithmetic over a long context — it's cheaper, repeatable, and auditable.

### 3. Qualitative analysis across many doors

This is where an LLM beats a spreadsheet:

- **Objection clustering** — "the three most common reasons people said no this month,
  with example quotes."
- **Pitch coaching** — if you record longer memos (or actual pitch recaps), Claude can
  compare what you said at doors that converted vs. doors that didn't.
- **Follow-up extraction** — every "come back later", "ask for my husband", "call in
  June" pulled into a single follow-up list with locations attached.
- **Weekly narrative reports** — a human-readable summary: areas worked, results,
  trends vs. last week, suggested focus for next week.

## What data would make the analysis much better

Everything below maps to a small future Gepsi feature (mostly extensions of the note
form). Ordered by impact:

1. **Structured outcome per note** — a one-tap enum at the door:
   `sale | no_answer | callback | rejected | not_home`. This is the single highest-value
   change: it removes all ambiguity from outcome extraction and makes conversion rates
   exact instead of inferred. Optional second field: rejection reason.
2. **One note per door, consistently** — even a 3-second "no answer" memo or a single
   tap. Analysis quality is bounded by logging discipline; doors with no note are
   invisible. (The current UI only allows notes while recording a route, which is the
   right habit already.)
3. **Interest score (1–5)** — one tap, turns "callback" lists into prioritized lists.
4. **Product discussed** — if you sell more than one thing.
5. **Follow-up flag + date** — "come back in May" as data, not prose.
6. **Address reverse-geocoding** — store the nearest street address with each note
   (osmdroid/Nominatim can do this) so reports say "Kupittaankatu 12" instead of
   coordinates.

A useful rule: anything you'd want to *filter or count by* should be a structured
field; anything narrative (what they said, vibe of the conversation) belongs in voice.
Voice memos are excellent raw material *because* Claude can structure them later — but
a field you tapped is always more reliable than a field inferred from audio.

## Concrete pipeline (works today)

The `.gepsi` export package added in v0.3 is already the analysis export: it contains
`manifest.json` (all routes, points, notes with timestamps and coordinates) plus every
voice memo as `audio/*.m4a`.

1. **Export** — share the package to yourself (Drive, email).
2. **Unzip** it on a computer; rename to `.zip` if needed.
3. **Transcribe** — e.g. `whisper audio/*.m4a --language fi --output_format json`.
   Keep the mapping from audio file name → transcript (file names appear in
   `manifest.json` under `notes[].audioFile`).
4. **Analyze with Claude** — give it `manifest.json` + the transcripts. Two good modes:
   - *Interactive*: drop the files into a Claude conversation (or Claude Code) and ask
     questions.
   - *Programmatic*: a small script that calls the Claude API once per week with the
     new data and a fixed analysis prompt, producing the weekly report automatically.

Example prompt to start from:

> Attached is `manifest.json` from my door-to-door sales tracking app (GPS routes with
> timestamped notes) and transcripts of the voice memos (file names match
> `notes[].audioFile`). For each note, classify the outcome
> (sale / no_answer / callback / rejected / unclear) and extract any objection or
> follow-up request. Then report: (1) outcome counts and conversion rate per route,
> (2) best and worst time-of-day based on note timestamps, (3) the top recurring
> objections with example quotes, (4) a prioritized follow-up list with coordinates
> and what to say when I return, (5) three concrete suggestions for next week.

### Privacy note

Notes and memos can contain other people's personal data (names, addresses, health or
contract details). Treat exported packages accordingly: don't share analysis exports
with third parties, and prefer local transcription (Whisper) over cloud speech APIs if
memos routinely contain identifying details.
