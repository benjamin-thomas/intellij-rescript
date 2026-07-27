---
summary: Register a ReScript live-template context so templates can be scoped to .res files
created: 2026-06-01
---

# Register a ReScript live-template context

## Background
Custom live templates (e.g. a "Tailwind Debug" snippet that inserts
`<p className="border-… border-…-500" />`) can't be scoped to ReScript
files because the plugin doesn't register a `TemplateContextType`. As a
result, "ReScript" doesn't appear in Settings → Editor → Live Templates →
Define, and any ReScript-targeted template either shows "No applicable
contexts" or has to fall back to the "Everywhere" context.

## Goal
Register a coarse, file-level `TemplateContextType` for ReScript so that
"ReScript" becomes a checkable context for live templates, scoped to `.res`
(and `.resi` if applicable) files.

## Implementation
- Add a `TemplateContextType` subclass, e.g. `ReScriptTemplateContextType`.
  - Presentable name: "ReScript".
  - `isInContext(...)` should return true when the file is a ReScript file.
    Prefer a file-type/language check using the plugin's existing
    `FileType`/`Language` singletons (find the canonical one in the codebase;
    do not hardcode a guessed class name). Example shape:
    `context.file.fileType == ReScriptFileType.INSTANCE`.
  - Use the `TemplateActionContext` overload of `isInContext` if the targeted
    platform version supports it; otherwise the `(PsiFile, offset)` overload.
- Register via the `com.intellij.liveTemplateContext` extension point.
  IMPORTANT: on current platform versions the id goes on the EP via the
  `contextId` attribute, NOT in the constructor:
  ```xml
  <liveTemplateContext contextId="ReScript"
    implementation="<pkg>.ReScriptTemplateContextType"/>
  ```
- Match existing code conventions (Kotlin vs Java, package layout).

## Acceptance Criteria
1. "ReScript" appears as a checkable context under Live Templates → Define.
2. A template with ONLY the ReScript context checked expands in `.res` files.
3. The same template does NOT expand in unrelated files (`.ts`, `.md`, etc.).
4. No regression to existing live-template behaviour.

## Notes / out of scope
- This is a coarse, whole-file context. It only inspects the file type, so it
  does NOT require descending into the PSI — the fact that JSX blocks are
  currently opaque is irrelevant here.
- A finer-grained context (e.g. "only inside a JSX expression") is explicitly
  out of scope and blocked on JSX being modelled in the PSI. Possible follow-up.
- Optional follow-up: if we ever want to ship the debug snippet bundled with
  the plugin (via `com.intellij.defaultLiveTemplates`), the template XML must
  include `<context><option name="ReScript" value="true"/></context>`
  referencing this same contextId.
