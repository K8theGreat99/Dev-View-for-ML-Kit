---
name: session-tracking-in-linear
description: Track Claude Code coding sessions as Linear issues in the K8theGreat workspace — root issue per branch, sub-issue per build, with a YAML header carrying branch, versionCode, versionName, and buildNumber. Use this skill whenever a coding session starts, a new build is produced, a branch merges to main, or the user mentions version codes, version names, build numbers, session logs, or tracking work in Linear — even if they don't name this skill explicitly.
---

# Session Tracking in Linear

Session tracking lives entirely in Linear issues.

**Workspace:** K8theGreat · **Team:** K8theGreat (`K8T-`) · **Project:** one per repository

> This skill is repo-agnostic. Anything specific to a single app — the version-name
> theme, the emoji convention, the naming rules — lives in that repo's `CLAUDE.md`.
> Read `CLAUDE.md` first, then apply the mechanics below.

---

## The model

* **One Claude Code context window = one working branch = one root Linear issue.**
* **Additional changes to the codebase in that same session = one sub-issue** of the root issue.
* Everything is **In Progress** until the branch merges to main, then **Done**. Changing status to Done can happen automatically on merge to main if you use a magic word in your commit message.

## What is worth tracking

Tracking exists to record what steps were taken in which session. It is not an audit
trail, and it is not that deep. Use judgment:

* **App code changed** → sub-issue. This is the main case.
* **A build with no app-code change** (CI config, workflow tweak) → no sub-issue. Let
  `buildNumber` climb on its own.
* **A major documentation change** — an overhaul, a rewritten skill, a new convention
  → worth an issue or sub-issue even though no app code moved.
* **A typo or a small doc fix** → not worth tracking.

When a case is genuinely borderline, ask the user rather than inventing a rule.

The only crucial aspect of session tracking is the versionCode. We always need a new entry (either issue or sub-issue) when there's a new versionCode.

## The YAML block — authoritative

Every issue, **including sub-issues**, opens its description with this block. It is the source of truth. The title is a display copy derived from it.

Use a fenced `yaml` code block, **not** `---` delimiters. Linear's editor rewrites a
`---` fence into a code block anyway, and a bare `---` is also Markdown for a
horizontal rule, so the fence is both what Linear stores and what renders correctly.

````
```yaml
branch: claude/branch-name-goes-here-efmznn
issueId: K8T-123
versionCode: 027
versionName: Example Name
buildNumber: 70
issueOverview: {Answer the question "what is this coding session about?" Ideally 1-3 sentences.}
```
````

* `versionCode` is zero-padded to three digits in the YAML, but not in the title.
* `branch` is the real GitHub branch name for the session.
* `issueId` is the Linear identifier (e.g. `K8T-123`) returned when the issue is created.
* `buildNumber` comes from GitHub at build time.
* The YAML block goes at the top. Truncated description previews in list queries must still expose it.

## Title format

```
{versionCode}. {short description} | {emoji} {versionName}
```

Example: `30. Fix export crash on Android 13 | 🔬 Example Name 4`

* `{emoji}` follows the repo's emoji convention — see that repo's `CLAUDE.md`. Never
  assume a theme carried over from another project.
* `{versionName}` follows the repo's naming theme — also in `CLAUDE.md`.
* Titles can run long — the project issue list view shows up to three lines.

---

## At session start

**1. Determine the version code and version name.**

Fetch the five most recently created issues in the current repo's project:

```
list_issues(project: "{project name}", orderBy: "createdAt", limit: 5,
            fields: ["id","title","description","status"])
```
//Update note: this search query template needs to be updated to also find only issues of the issue type session. this is accomplished by searching for items with a session label. but before this can become effective, we need to check all of this project's session tracking issues and apply the session label to them if it is not present.

Read the `versionCode` out of each YAML block. The highest number is the current versionCode. Make sure the next versionCode you create is higher.

**Scope every lookup to the current repo's project.** versionCode, versionName, and
buildNumber counters are per-project. Issues in other projects are unrelated.

If the project has no session issues yet, this is session one: start `versionCode` at `001`.

Check the status of each session-type issue returned. If any are "in progress" consult with the user for guidance to avoid conflicts.

If the user states a version code explicitly — which they will do when more than one branch is active — theirs wins.

Choose a new `versionName` for the session based on the repo's naming theme in `CLAUDE.md`.

**2. Create the root issue.**

* Title per the format above
* Description: YAML block, then the log skeleton below
* Priority: **High** (2)
* Status: **In Progress**
* Project: the repo's project
* Label: session

Linear returns the issueID in the create response — grab it now and put it in the yaml.

```
## Log notes
**What I worked on**
**Learnings**
**Workflow notes / things to improve**
```

The log notes area goes below the yaml and belongs to the user. Set up the skeleton and then leave it alone — do not write summaries into it.

---

## During the session — each additional build with a change to the codebase

Create a **sub-issue** of the root issue:

* `parentId`: the root issue
* Title: per the format above
* Priority: **No priority** (0)
* Status: **In Progress**
* Description: its own YAML block — same `branch`, new `versionCode`, suffixed `versionName`, new `buildNumber`, and its own `issueId` filled in from the create response the same way as the root issue

Remember, you do not have to create a sub-issue for insignificant changes.

---

## At merge

The branch is pushed to main (fast-forward, no PR), so nothing closes on its own — the final commit message has to do it.

Read `issueId` out of the root issue's YAML block and out of every sub-issue's block, then list them all after a closing magic word in the final commit message:

```
Fixes K8T-120, K8T-121, K8T-122
Session 27 — Example Name. Export crash fix plus two follow-up builds.
```

The magic word must come before the IDs, and Linear only acts on it once the commit lands on main. Any of `fixes` / `closes` / `resolves` works.

---

## Notes

* Use comments as you see fit.
* A Linear document copy of this skill may exist in the workspace. **This repo file is
  the authoritative version**; treat the Linear document as a draft.
