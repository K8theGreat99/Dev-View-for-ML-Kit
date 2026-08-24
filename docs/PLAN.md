# Dev View for ML Kit — Plan

Working notes, not a spec. Decisions get written here as they are made, with the
reasoning behind them, so we do not re-argue settled questions in a later context
window. Anything still undecided is listed under [Open questions](#open-questions).

---

## What this app is

Dev View is a **diagnostic bench for ML Kit's OCR**, not a scanning app.

You feed it images. It runs ML Kit text recognition on each one and shows you the
image together with ML Kit's actual output. Images accumulate into a scrollable
gallery, like a camera roll.

The point is reconnaissance *before* you write parsing code. Load ten receipts from
the same store, scroll through, and see whether ML Kit puts the total in the same
place every time, how it orders columns, where it breaks lines. Then you know what
you are actually parsing against.

**Non-goals.** Dev View does not clean up, correct, or interpret OCR output. It does
not try to be a document scanner. It shows what ML Kit really returns.

---

## What ML Kit actually returns

This shapes most of the design, so it is worth stating plainly.

ML Kit returns a **four-level tree**:

```
Text
└── TextBlock       (a paragraph-ish region)
    └── Line
        └── Element (roughly a word)
            └── Symbol (roughly a character)
```

Every node carries its text, a **bounding box** in image pixel coordinates, a
confidence score, and a detected language.

There is also a flat `Text.text` string. **It is derived, not a second data source** —
essentially the block texts joined with newlines. The tree can reproduce it exactly in
one line of code; the reverse is impossible.

### Two consequences that drive the UI

1. **The flat string cannot tell you where blocks end.** Lines within a block are
   joined by a newline, and blocks are *also* joined by a newline. A `\n` in the flat
   text is ambiguous — you cannot tell which boundary you just crossed. That ambiguity
   is often exactly the structure you were hoping to parse against.

2. **Block order is not reading order.** ML Kit returns blocks in detection order,
   which is roughly top-to-bottom but genuinely jumps around on multi-column layouts,
   receipts with a price column, or anything with a sidebar. Parsers that assume
   blocks read like a book break here. This is the single most valuable thing Dev View
   can make visible.

---

## Decisions made

### Show the tree and the JSON. Not the flat text.

- **Tree view** — the human view. Indented, collapsible, color-coded by level.
- **JSON view** — the LLM view. Full structure including bounding boxes and confidence.
- **No flat-text view.** It is derived from the tree, so displaying it costs a tab and
  attention to show data we already have. If it is ever genuinely needed it is a
  one-line addition.

The tree with coordinates *is* what a program receives from ML Kit. The flat string is
a convenience shortcut that discards structure before your code ever sees it.

### Bounding-box overlay on the image

Draw each node's box over the image, labeled with its index. This is what makes the
block-ordering problem visible at a glance — you see immediately that block 4 is off
to the right and block 2 sits below block 5.

Tapping a box highlights that node in the tree, and vice versa. Overlay granularity
toggles between blocks, lines, and words.

### Stable node IDs

Every node gets a path ID: `B2.L1.E3` = block 2, line 1, element 3.

Small decision, large payoff. Comments anchor to an ID, exports reference IDs, and an
LLM reading an export can connect a remark to the exact node and its coordinates.
Without this, comments drift away from the output they describe.

### Export as Markdown with fenced JSON

Markdown structure is read reliably by LLMs; the fenced JSON preserves exact fidelity.
Comments appear as labeled sections tied to node IDs.

**Batch export matters most.** Ten samples of the same layout in one document, with a
batch-level note, is the artifact that answers "is this reliable across samples?"

### Coordinate scaling

If an image is downscaled before OCR or display, bounding boxes must be scaled to
match. ML Kit's coordinates are relative to the bitmap it was handed. Getting this
wrong makes the overlay silently drift.

---

## Phases

**Phase 0 — Skeleton and APK pipeline.** ✅ Complete (build 3)

Gradle project, Compose, Material 3, GitHub Actions producing a downloadable APK.
Deliberately built and verified before any feature code, because CI and signing are
where a first Android setup usually stalls. An early About screen proves version
metadata travels from Gradle through CI onto the device.

**Phase 1 — Core loop.** ← current

Pick images → OCR → gallery → inspect.

- Multi-select image picker
- Images copied into app storage so the gallery is durable
- ML Kit on-device text recognition (offline, no API key, no cost)
- Room database for persistence
- Gallery: scrollable grid of thumbnails
- Detail: image on top, Tree and JSON tabs below
- About screen with the version's scientist bio

**Phase 2 — The visual layer.**

Bounding-box overlay labeled with indices; tap-to-link between box and tree; overlay
granularity toggle; pinch to zoom.

**Phase 3 — Annotation.**

Comments at image level and at any node. Visual indicators in the tree for annotated
nodes.

**Phase 4 — Collections and export.**

Named batches. Markdown export via share sheet, file, or clipboard. Single image or
whole batch.

---

## Technical decisions

| Area | Choice | Why |
|---|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 | Current standard for Android |
| minSdk | 26 | Wide compatibility at no real cost |
| compileSdk / targetSdk | 34 | Matches the test device (Android 14) |
| OCR | ML Kit Text Recognition v2, on-device | Offline, free, no API key |
| Persistence | Room | Structured queries over stored OCR results |
| Images | Copied into app-internal storage | Picker URIs are transient; the gallery must outlive them |
| Image picking | Android photo picker | No storage permission required |
| Build type shipped | **debug** | Signed with the standard debug keystore, so it installs with no signing secrets. A release build would be unsigned and refuse to install. |
| Distribution | GitHub Release asset | A direct link that works in a phone browser; workflow artifacts are zipped and need a login |

`buildNumber` is injected from the GitHub Actions run number, so every APK carries the
build number recorded in its Linear issue.

---

## Conventions

Version names are **women in science and technology**, alphabetical, each with a paired
emoji and a bio shown on the About screen. See `CLAUDE.md`.

Git: solo repo, no pull requests, one context window per branch, fast-forward merges to
main only on explicit go-ahead. See `CLAUDE.md`.

Session tracking lives in Linear. See `.claude/skills/session-tracking-in-linear/`.

---

## Open questions

Not blocking Phase 1; decide when they come up.

- **Sort and filter in the gallery** — by date, by collection, by block count?
- **Re-running OCR** on an image already in the gallery, to compare across ML Kit
  versions.
- **Confidence display** — always visible, or only on request? It is noisy at the
  symbol level.
- **Very large images** — downscale before OCR for memory, and if so, at what
  threshold? Affects coordinate scaling.
- **Export scope** — does the export include the image itself, or only text and
  coordinates? Embedding images makes the document large but self-contained.
- **Camera capture** — currently upload-only, by design. Revisit if it turns out to be
  a nuisance in practice.
