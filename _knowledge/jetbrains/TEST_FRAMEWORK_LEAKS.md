# Test-framework leaks across one JVM

Gradle runs the whole suite in a single JVM (no `tasks.test { forkEvery }`), so
`ParsingTestCase` and `BasePlatformTestCase` share one process. They do NOT share
one application: `ParsingTestCase` installs a lightweight mock, `BasePlatformTestCase`
needs the real platform. Anything cached in a JVM-wide static during the mock's
lifetime outlives it.

## The one that bit us

`ReScriptBraceMatcherTest`'s three tests failed together, intermittently
(~3 runs in 11), and passed on an immediate rerun with identical sources.

Chain:

1. `GeneratedParserUtilBase` calls `LanguageBraceMatching.INSTANCE.forLanguage(...)`
   on **every** parse — so every parser test triggers it.
2. Under the mock application there is no registered brace matcher, so the lookup
   returns nothing.
3. `LanguageBraceMatching` is a `KeyedExtensionCollector`, which caches that empty
   result in a **JVM-wide static**. Nothing invalidates it when the mock app dies.
4. A later `BasePlatformTestCase` asks for the brace matcher, gets the cached
   nothing, and typing `{` no longer auto-closes.

The intermittency was never randomness — it is Gradle reshuffling test-class order
on recompile. Parser-tests-first fails every time; almost any other platform test
running first warms the collector with a real app and immunises the JVM.

## The fix

Register the extension explicitly in the `ParsingTestCase` subclass:

```kotlin
override fun setUp() {
    super.setUp()
    addExplicitExtension(LanguageBraceMatching.INSTANCE, ReScriptLanguage, ReScriptBraceMatcher())
}
```

This does double duty: it supplies the real matcher under the mock app, and both
`addExplicitExtension` and its teardown call `clearCacheForLanguage`, so the static
cache is purged on the way in and on the way out.

**Any new `ParsingTestCase` subclass needs the same line.**

`forkEvery = 1` also fixes it but costs ~2.7× wall clock (93s vs 35s here) and
introduced its own failures under load. Not worth it for one known coupling.

## The general shape

If a platform test fails only in a full-suite run, passes alone, and fails as a
whole class at once, suspect a `KeyedExtensionCollector` (or similar static cache)
poisoned by a mock-application test earlier in the JVM. Reproduce by forcing class
order rather than re-running and hoping.

## Related: don't bypass `parseFile`

`ParsingTestCase.parseFile(name, text)` runs `doSanityChecks`, which is where
`ensureParsed`, `checkRangeConsistency` and `ensureCorrectReparse` live — the last
of these re-lexes from scratch and demands a byte-identical result, which is
exactly the contract the packed restart state exists to satisfy. Calling
`createPsiFile` directly parses the file but skips all of it, silently. Use
`parseFile`.
