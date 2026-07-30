---
summary: ReScriptBraceMatcherTest fails intermittently and passes on immediate rerun
created: 2026-07-30
---

# Brace-matcher test flake

## Goal
Make `ReScriptBraceMatcherTest` deterministic, or prove it is a platform-test-fixture
issue and document it.

## Context
Observed three times during the JSX relay (`grammar/060`), across different cycles and
different agents, always the same trio:

    testCurlyBraces / testParentheses / testSquareBrackets

Each time: all three fail in one full-suite run, then pass on an immediate rerun with
byte-identical sources. RED and GREEN runs bracketing the same grammar produced
different results, so it is provably unrelated to grammar changes.

Suspect shared mutable state or ordering dependence in the fixture setup, or a
platform test-framework race. It is not JSX-specific — the tests predate this work.

## Acceptance Criteria
1. Root cause identified, or ruled out with evidence.
2. The suite passes reliably across repeated consecutive runs (say 10).
3. If the cause is a platform quirk rather than our code, it is documented in
   `_knowledge/` so the next person does not re-investigate.
