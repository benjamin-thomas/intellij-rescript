---
summary: GotoRelatedProvider — file-level navigation without custom keybindings
updated: 2026-03-31
relates: [architecture]
---

# GotoRelatedProvider

Provides items for the "Go to Related Symbol" action (Ctrl+Alt+Home or
platform-dependent shortcut).

IntelliJ calls `getItems(psiElement)` with the current file. We return the
counterpart file (`.res` ↔ `.resi`). The platform handles the keybinding — we
don't define shortcuts, so it works regardless of the user's keymap.
