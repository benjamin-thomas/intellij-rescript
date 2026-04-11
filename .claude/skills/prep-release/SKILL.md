---
name: prep-release
description: Prepare a version release of the intellij-rescript plugin. Investigate commits since the last release, draft the CHANGELOG entry, bump the version, run the test and plugin-config checks, then hand off the git and publish commands to the user. Never executes git operations or publishes.
---

# Prep Release

A skill for preparing a new version release of the `intellij-rescript` plugin.

## Hard rules

These are non-negotiable. Violating any of them is a bug in the skill execution.

- **Never run git commands.** Not `git commit`, not `git tag`, not `git push`, not `git stash`. The user is in a sandboxed environment and runs all git operations themselves. You may run **read-only** git commands like `git status`, `git log`, `git diff`, `git show`, `git for-each-ref`, `git tag -l` — these are safe and required for investigation.
- **Never run the release script.** `manage/prod/release-plugin` publishes to the JetBrains Marketplace and is the most irreversible step in the flow. It is the user's job.
- **Never modify files the user hasn't approved.** The only files this skill touches without asking are `build.gradle.kts` (version bump) and `CHANGELOG.md` (new section) — and only **after** the user has approved the draft release content. `GOAL.md` updates, if any, must be explicitly proposed first.
- **Never use `git add -A` or `git add .`** in the hand-off commands you print. Only stage the specific files that were touched.
- **Abort on a dirty working tree.** Do not attempt to stash, commit, or otherwise "help" the user clean up. Print the diagnostic and stop.
- **Abort on any test or config-check failure.** Do not attempt to fix the failure inside this skill. Leave the working tree in its partially-edited state so the user can inspect, and explain what failed.

## Phase 1: Validate preconditions

Run these checks **before** doing any investigation work. If any fails, stop immediately with the diagnostic shown.

1. **Working tree is clean.** Run `git status --porcelain`. It must be empty (untracked `.idea/` is OK — ignore it if it's the only thing).

   If dirty, stop and say:
   > The working tree has uncommitted changes. A release commit should only contain the version bump and CHANGELOG update — never feature work.
   >
   > Typical flow: finish your feature, commit it with a message like `"Prep for release"` (or whatever describes the feature), then re-run `/prep-release`.
   >
   > Here's what's uncommitted: [show the output of `git status --short`]

2. **On the main branch.** Run `git branch --show-current`. It should be `master` (this project's main). If it's something else, warn but allow the user to proceed — some projects tag releases off feature branches, and you shouldn't block that without asking.

3. **There's at least one commit since the last release.** Find the last release tag via `git tag -l 'v*' --sort=-version:refname | head -1`, then check `git rev-list <tag>..HEAD --count`. If zero, stop and say "no commits since v<last> — nothing to release."

## Phase 2: Investigate

This is the phase where you earn your keep. Do **not** ask the user what the release is about — find out.

1. **Current version.** Read `build.gradle.kts` and find the line matching `^version = "..."`. Capture both the line number and the value. The version lives at the top of the file (around line 13 as of v0.4.0) but **don't hardcode the line number** — grep for it. The line may drift as the build file evolves.

2. **Last release version and date.** From the most recent `v*` tag: `git show <tag> --no-patch --format='%H %ci %s'`. Also run `git for-each-ref refs/tags/<tag>` to confirm it's a **lightweight** tag (object type `commit`, not `tag`). This project's convention is lightweight; matching it is important so `git show <tag>` works the way the user expects.

3. **Commits since last release.** Run `git log v<prev>..HEAD --oneline` and `git log v<prev>..HEAD --stat` (the second one shows which files changed per commit — crucial for judging user-facing vs internal). Read the output yourself, don't ask the user to summarize.

4. **CHANGELOG format.** Read `CHANGELOG.md`. Note the section structure:
   - `## v<X.Y.Z> — <Title>` as section headers
   - `### <Area>` subsections (e.g. `Parser`, `Editor features`, `Navigation`, `LSP`, `Internal`)
   - Bullets with **bold lead terms** for features, plain prose for fixes and polish
   - Honest framing over oversell — known limitations should be stated, not hidden
   This format is parsed by the `org.jetbrains.changelog` plugin and becomes the marketplace listing's "change notes," so getting it right matters.

5. **GOAL.md, if it exists.** Read it. If it has checklists or phase trackers that plausibly relate to commits in this release, note which items might need updating. Prior release commits have sometimes touched GOAL.md (e.g. `51c3dc8`). You'll propose any updates in Phase 3.

6. **Relevant source files.** For any commit whose subject isn't self-explanatory, read the actual diff (`git show <sha>`) to understand what changed. Don't guess. The user is counting on you to produce a CHANGELOG entry they don't have to rewrite.

## Phase 3: Propose

Now you draft the release content. Present it as **one cohesive proposal**, not a series of questions. The user wants to approve/edit, not fill in blanks.

Your proposal must include:

### a. Version bump

Recommend patch / minor / major based on the commits. Guidance:
- **Patch** (`0.4.0 → 0.4.1`): bug fixes, polish, internal refactors, completing partial features, removing false positives in existing features. No new user-facing capability surface.
- **Minor** (`0.4.0 → 0.5.0`): new user-facing features, new editor actions, new grammar coverage that genuinely adds capability.
- **Major** (`0.4.0 → 1.0.0`): reserved; don't propose unless the user has indicated a maturity milestone.

State your reasoning in one sentence ("I'm proposing a patch because the commits are fixes to existing parser coverage — no new feature surface") and offer the user a clean override: "Override with `minor` or `major` if you'd prefer."

### b. Title

Propose a short title matching the existing convention (`v0.4.0 — Language Injection`, `v0.2.1 — Fixes & Polish`, etc.). The user has said they don't particularly care about titles, so propose one and move on — don't agonize. Make it descriptive of the *primary* change, not a laundry list.

### c. Changelog bullets

Draft the full section, grouped by area in the project's existing style. Rules:

- **Lead with user impact, not implementation detail.** "Top-level `switch` no longer flagged as a parse error" is better than "Added `TopLevelExpr` rule to grammar."
- **Bold the lead term** for features/capabilities. Plain prose for fixes.
- **Be honest about limitations.** If a feature is partial, say "partial support" and explain in one sentence what works and what doesn't. The "partial support" phrasing from v0.4.1 is a good template — see the entry in CHANGELOG.md for reference.
- **Omit internal-only changes.** Renames, refactors, test additions, knowledge-base updates — none of these belong in the user-facing changelog unless they have observable effects. If you're unsure, leave it out.
- **Don't promise future work** in binding language. Soft signals ("will be lifted once...") are OK when they explain *why* a limitation exists; hard promises ("coming in v0.5.0") are not.

### d. GOAL.md updates (only if applicable)

If Phase 2 turned up GOAL.md items that should be updated, propose the exact text changes. If nothing in GOAL.md relates to this release, skip this entirely — don't invent work.

### e. Present and wait

Show the whole proposal as one block and ask: **"Approve this release draft? (y / edit / N)"**

- `y` — proceed to Phase 4
- `edit` — the user will tell you what to change; iterate until they approve
- `N` — stop, release is cancelled, no files touched

## Phase 4: Apply mechanical changes

Only after explicit approval from Phase 3.

1. **Bump the version** in `build.gradle.kts` using the `Edit` tool. Match the exact `version = "<old>"` line; don't touch anything else.

2. **Prepend the new section** to `CHANGELOG.md`. Insert it immediately below the `# Changelog` header and above the previous `## v<prev>` section. Use the `Edit` tool with `old_string` matching `# Changelog\n\n## v<prev>` for uniqueness.

3. **Apply GOAL.md changes** if any were proposed and approved.

4. **Run `./gradlew test`.** Stream the output; pipe through `grep -E "(FAILED|BUILD|tests completed)" | head` to keep the context tight. If the build fails, **stop**. Print the failure and leave the working tree alone. Do not attempt to revert your edits; the user will inspect and decide.

5. **Run `./gradlew verifyPluginProjectConfiguration`.** Same discipline — stop on failure.

## Phase 5: Hand off

Once both checks pass, produce the hand-off report. This is the final output of the skill.

1. **Show the diff.** Run `git diff CHANGELOG.md build.gradle.kts` (add `GOAL.md` if you touched it) and show the output. The user will use this to review your work.

2. **Print the exact commands the user needs to run**, in order, as a copy-pasteable block. Example shape:

   ```bash
   # 1. Review the diff above, then stage and commit
   git add CHANGELOG.md build.gradle.kts
   git commit -m "Release v<X.Y.Z>"

   # 2. Tag (lightweight — matches v0.2.0–v0.4.x convention)
   git tag v<X.Y.Z>

   # 3. Sanity check the tag points at the release commit
   git show v<X.Y.Z> --stat

   # 4. Push commit and tag
   git push
   git push origin v<X.Y.Z>

   # 5. Publish to JetBrains Marketplace
   ./manage/prod/release-plugin
   ```

3. **Remind about ordering trade-offs.** The user may want to tag *before* publishing (safer — if the marketplace publish fails, the tag still marks the intended release), or publish first and tag only on success (keeps tags aligned with what's actually in marketplace). Point this out; don't decide for them.

4. **Stop.** Do not follow up by asking "want me to run any of these for you?" — the answer is always no. The skill's job is done.

## Lessons baked in from prior releases

Context that future-you (another Claude instance executing this skill) should know without having to rediscover:

- **Tags are lightweight**, not annotated. Verify with `git for-each-ref refs/tags/<tag>` — object type should be `commit`.
- **Release commits are clean**: they touch only `CHANGELOG.md`, `build.gradle.kts`, and sometimes `GOAL.md`. Feature work is *always* a separate preceding commit. If the user's working tree has mixed concerns, the pre-condition check in Phase 1 will catch it.
- **The version line** is `version = "X.Y.Z"` in `build.gradle.kts` at roughly line 13. Grep for the exact line; don't hardcode.
- **The CHANGELOG is load-bearing**: `org.jetbrains.changelog` reads it at build time and injects the section into `patchPluginXml.changeNotes`, which becomes the marketplace listing. Bad formatting there means bad marketplace UX.
- **`verifyPluginProjectConfiguration`** is flagged in this project's `CLAUDE.md` as the config-correctness check. Run it every time.
- **Honest framing over oversell.** v0.4.1 established the "partial support" convention for features that work but have known limitations. Carry it forward.
- **The release script is interactive.** It uses `pass` for the marketplace token, runs `clean → test → buildPlugin → publishPlugin`, and prompts the user before publishing. The user is aware of this; don't re-explain it in the hand-off report.
