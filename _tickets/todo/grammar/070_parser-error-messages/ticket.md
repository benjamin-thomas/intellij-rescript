---
summary: Replace GrammarKit's expected-token laundry-lists with readable error messages
created: 2026-07-30
---

# Readable parser error messages

## Goal
Make `PsiErrorElement` messages readable, since they surface in the editor as tooltips.

## Context
GrammarKit emits the full expected-token set on failure. A real example from the JSX
recovery fixtures:

    "<and let binding>, AMPAMP, AMPAMPAMP, ARROW, AS, ASYNC, AWAIT, BANG, BANGEQ,
     BANGEQEQ, BIGINT, BLOCK_COMMENT, ... and ... expected, got '</'"

Surfaced during `grammar/060`. Both Fable and Sol independently flagged it; Sol's
recommendation was a follow-up ticket rather than inline work, which is this.

GrammarKit supports `name=` attributes on rules to humanize these. Worth checking how
far that gets before considering a custom `ParserDefinition`-level approach.

## Acceptance Criteria
1. Error messages for common mid-edit JSX cases read as sentences, not token dumps.
2. The recovery golds are updated and remain structurally unchanged.
3. Approach documented in `_knowledge/parser/` — this affects every future rule.
