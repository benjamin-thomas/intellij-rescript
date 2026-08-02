package com.github.benjamin_thomas.intellij_rescript.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%

%class _ReScriptLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
    // IntelliJ gives the lexer a single int for restart state, so we pack our
    // restart context into bit fields inside that int (layout v3):
    //   bits 0..3    = JFlex lexical state, halved (see LEXICAL_STATE_MASK)
    //   bit  4       = a line break separates us from the last significant token
    //   bits 5..6    = block-comment nesting depth, clamped at 3
    //   bit  7       = previous significant token is an "expression end"
    //   bits 8..31   = context stack: three 8-bit frames, low frame = innermost
    //
    // Every bit is spoken for. A new signal has to shrink a field first — which
    // is how bit 4 was won: JFlex numbers lexical states in steps of two, so
    // halving the state loses nothing and buys room for 16 of them.
    //
    // A context frame remembers why a `{` region was opened, so the matching
    // `}` can restore the right lexer state. Frame = 2-bit kind (high bits)
    // + 6-bit brace depth. Depth is >= 1 while a frame is live, so a live
    // frame's byte is never zero and 0x00 unambiguously means "empty slot".
    //
    // The context stack is NOT stored here — see the `frames` field. Only a
    // truncated copy of it rides in bits 8..31, as a hint nothing reads back.
    //
    // Nothing saturates while lexing. Every counter here is a clamp applied on
    // the way into the int and nowhere else, so the widths above bound what a
    // restart can describe, never what the lexer can read.

    // Holds a HALVED lexical state: JFlex allocates two DFA slots per declared
    // state, so `yybegin` only ever sees even ids (0, 2, ... 22 for today's 12
    // states). packRestartState asserts that rather than trusting it.
    // Public so a test can assert the block-comment state is never observable
    // at a token boundary — the property the blockCommentReturn field rests on.
    public static final int LEXICAL_STATE_MASK = 0xF;
    private static final int COMMENT_DEPTH_MASK = 0x3;
    private static final int CONTEXT_STACK_MASK = 0xFFFFFF;

    private static final int SAW_LINE_BREAK_SHIFT = 4;
    private static final int COMMENT_DEPTH_SHIFT = 5;
    private static final int PREV_IS_EXPR_END_SHIFT = 7;
    private static final int CONTEXT_STACK_SHIFT = 8;

    // Public so tests can ignore this bit when asserting zero-state restart
    // boundaries: the bit is set by ordinary tokens (any identifier or literal
    // sets it) and self-corrects on the first significant token after restart.
    public static final int PREV_IS_EXPR_END_MASK = 1 << PREV_IS_EXPR_END_SHIFT;

    // Also public for that assertion, but for the opposite reason: this bit does
    // NOT self-correct — a restart landing on the `<` it governs would decide the
    // token wrongly without it, which is exactly why it is packed. It is
    // ignorable only for the zero-state check, which asks about lexical regions.
    public static final int SAW_LINE_BREAK_MASK = 1 << SAW_LINE_BREAK_SHIFT;

    private static final int CONTEXT_FRAME_BITS = 8;
    private static final int CONTEXT_FRAME_MASK = 0xFF;
    private static final int FRAME_KIND_SHIFT = 6;
    private static final int FRAME_DEPTH_MASK = 0x3F;

    // Frame kinds. TEMPLATE: a `${...}` interpolation — its closing brace
    // returns to the template state. JSX_ATTR: a `{...}` attribute expression
    // (or spread) inside an opening tag — its closing brace returns to JSX_TAG.
    // JSX_CONTENT: one children region; its payload is split into a count of
    // consecutively nested unbraced elements (`<div><ul><li>` = count 3, bits
    // 3..5) and the brace depth of an open `{child expr}` (bits 0..2). The
    // frame is popped when the count returns to zero, i.e. on the outermost
    // closing tag.
    private static final int FRAME_KIND_TEMPLATE = 0;
    private static final int FRAME_KIND_JSX_ATTR = 1;
    private static final int FRAME_KIND_JSX_CONTENT = 2;
    // A `(` or `[` opening an UNBRACED attribute value (`b=f(x)`, `b=[1, 2]`).
    // Same shape as JSX_ATTR — the interior is ordinary expression text and the
    // closer that brings the depth to zero returns to JSX_TAG — but a distinct
    // kind, so only these regions make `)`/`]` region-closing rather than plain
    // operators. Closers are counted against one depth, never matched to their
    // opener, so `b=f(1]` closes the region too — mis-scoping only input that is
    // already invalid.
    //
    // This exhausts the packed 2-bit kind field. A fifth kind needs a layout
    // change, not another constant.
    private static final int FRAME_KIND_JSX_VALUE = 3;

    // TEMPLATE frame payload: 2-bit return-state selector (bits 4..5) over a
    // 4-bit brace depth (bits 0..3). The selector remembers which template
    // state the interpolation's closing `}` must return to — a template can
    // be a plain expression, a JSX attribute value, or a JSX child.
    private static final int TEMPLATE_DEPTH_MASK = 0xF;
    private static final int TEMPLATE_RETURN_SHIFT = 4;
    private static final int TEMPLATE_RETURN_MASK = 0x3;
    private static final int TEMPLATE_RETURN_TOP = 0;
    private static final int TEMPLATE_RETURN_TAG = 1;
    private static final int TEMPLATE_RETURN_CHILD = 2;

    private static final int JSX_CONTENT_DEPTH_MASK = 0x7;
    private static final int JSX_CONTENT_COUNT_SHIFT = 3;
    private static final int JSX_CONTENT_COUNT_MASK = 0x7;

    // Whether the previous significant token is an "expression end" — drives
    // regex-vs-division disambiguation for `/` and JSX-vs-comparison for `<`.
    // Packed into the restart int (bit 7) so a restart in the middle of e.g.
    // `x / y / z` still knows the `/` follows a value and must be division,
    // not a regex start.
    private boolean prevIsExprEnd = false;

    // Whether a line break has been passed since the last significant token.
    // ReScript disambiguates `<` on exactly this, and the break may hide inside
    // a block comment (`x /* c` NEWLINE `*/ <div />` is JSX to bsc) — so
    // whitespace runs and comment interiors report here. String and template
    // interiors deliberately do not: a newline inside the previous *token* is
    // not a break (`"a` NEWLINE `b" <c` is a comparison to bsc), and their END
    // tokens clear the flag through track() anyway.
    private boolean sawLineBreak = false;
    private int commentDepth = 0;

    // Which state a block comment returns to. Live only, never packed, and that
    // is sound rather than lucky: no rule active in IN_BLOCK_COMMENT returns a
    // token except the terminating `*/`, which leaves the state in the same
    // action — so an entire nested comment is consumed inside one advance() and
    // no token boundary can land inside one. A restart only ever lands on a
    // token boundary, so this field is always written before it is read.
    //
    // Note the invariant also rests on file layout: the catch-all
    // `[^] { return BAD_CHARACTER; }` at the very bottom is active in every
    // state, and only loses the tie with IN_BLOCK_COMMENT's own `[^]` because
    // JFlex breaks equal-length matches by rule order. Moving it above that
    // block would make every comment character a token — loudly, but it would
    // also invalidate the reasoning above.
    //
    // Strings and templates could NOT do this: they emit tokens mid-state
    // (STRING_START, STRING_CONTENT, …), so their boundaries are observable and
    // their return state really does have to survive a restart — which is why
    // IN_TAG_STRING / IN_CHILD_STRING exist and no such clones are needed here.
    private int blockCommentReturn = YYINITIAL;

    // Every `/*` rule does the same three things. yystate() is the origin state
    // here because the read happens before the yybegin below — which is what
    // lets one helper serve all four entry points instead of each naming its
    // own state. A `/*` rule added to a MULTI-state block would still be
    // correct: it would record whichever of those states it was entered from.
    private void beginBlockComment() {
        commentDepth = 1;
        blockCommentReturn = yystate();
        yybegin(IN_BLOCK_COMMENT);
    }

    // The frame stack, and the source of truth for forward lexing. It is NOT
    // bounded by what the restart int can hold: the packed form carries only
    // the innermost PACKED_FRAMES frames, each counter clamped to the width it
    // has there (see packContextStack).
    //
    // Truncating the packed form is safe because nothing reads it back. Any
    // live frame makes the restart int non-zero; the platform only restarts
    // where the state equals the lexer's initial state (0); and for a
    // non-RestartableLexer `LexerEditorHighlighter` stores just the sign of a
    // token index — `ShortBasedStorage.unpackStateFromData` throws. So a state
    // that cannot be represented exactly is a state nothing can restart into.
    // `isRestartStateExact()` reports the difference and the lexer tests assert
    // that an inexact boundary is always non-zero.
    //
    // Frames are full-width here, which is what removes the caps that broke
    // valid code: three live regions, and seven consecutively nested unbraced
    // elements (real ReScript reaches nine).
    private int[] frames = new int[16];
    private int frameCount = 0;

    // In-memory frame layout. Deliberately wider than the packed one and laid
    // out differently, so the two cannot be confused: lowerFrame / raiseFrame
    // are the only translation between them.
    private static final int LIVE_KIND_SHIFT = 28;
    private static final int LIVE_DEPTH_MASK = 0x3FFF;          // bits 0..13
    private static final int LIVE_COUNT_SHIFT = 14;             // bits 14..27
    private static final int LIVE_COUNT_MASK = 0x3FFF;
    private static final int LIVE_ONE_CHILD = 1 << LIVE_COUNT_SHIFT;
    private static final int LIVE_TEMPLATE_RETURN_SHIFT = 14;   // templates carry no count

    private static final int PACKED_FRAMES = 3;

    private boolean hasFrame() {
        return frameCount > 0;
    }

    private int topFrame() {
        return frames[frameCount - 1];
    }

    private void setTopFrame(int frame) {
        frames[frameCount - 1] = frame;
    }

    private int topFrameKind() {
        return topFrame() >>> LIVE_KIND_SHIFT;
    }

    // Depth of the top TEMPLATE or JSX_ATTR frame (JSX_CONTENT frames have a
    // split payload and their own accessors below).
    private int topFrameDepth() {
        return topFrame() & LIVE_DEPTH_MASK;
    }

    private int topTemplateReturnState() {
        int selector = (topFrame() >>> LIVE_TEMPLATE_RETURN_SHIFT) & TEMPLATE_RETURN_MASK;
        if (selector == TEMPLATE_RETURN_TAG) return IN_TAG_TEMPLATE;
        if (selector == TEMPLATE_RETURN_CHILD) return IN_CHILD_TEMPLATE;
        return IN_TEMPLATE;
    }

    private void pushLiveFrame(int frame) {
        if (frameCount == frames.length) {
            frames = java.util.Arrays.copyOf(frames, frames.length * 2);
        }
        frames[frameCount++] = frame;
    }

    private void pushTemplateFrame(int returnSelector) {
        pushLiveFrame((FRAME_KIND_TEMPLATE << LIVE_KIND_SHIFT)
            | (returnSelector << LIVE_TEMPLATE_RETURN_SHIFT) | 1);
    }

    private void pushJsxAttrFrame() {
        pushLiveFrame((FRAME_KIND_JSX_ATTR << LIVE_KIND_SHIFT) | 1);
    }

    private void pushJsxContentFrame() {
        pushLiveFrame((FRAME_KIND_JSX_CONTENT << LIVE_KIND_SHIFT) | LIVE_ONE_CHILD);
    }

    private void pushJsxValueFrame() {
        pushLiveFrame((FRAME_KIND_JSX_VALUE << LIVE_KIND_SHIFT) | 1);
    }

    private boolean topIsJsxValue() {
        return hasFrame() && topFrameKind() == FRAME_KIND_JSX_VALUE;
    }

    /** Closes an unbraced attribute value's region when its depth reaches zero. */
    private void closeJsxValueDelimiter() {
        if (!topIsJsxValue()) return;
        decrementTopFrameDepth();
        if (topFrameDepth() == 0) {
            popFrame();
            yybegin(JSX_TAG);
        }
    }

    private void popFrame() {
        if (frameCount > 0) frameCount--;
    }

    private void incrementTopFrameDepth() {
        if (topFrameDepth() < LIVE_DEPTH_MASK) setTopFrame(topFrame() + 1);
    }

    private void decrementTopFrameDepth() {
        if (topFrameDepth() > 0) setTopFrame(topFrame() - 1);
    }

    private boolean topIsJsxContent() {
        return hasFrame() && topFrameKind() == FRAME_KIND_JSX_CONTENT;
    }

    private int jsxContentBraceDepth() {
        return topFrame() & LIVE_DEPTH_MASK;
    }

    private int jsxContentChildCount() {
        return (topFrame() >>> LIVE_COUNT_SHIFT) & LIVE_COUNT_MASK;
    }

    private void incrementJsxContentBraceDepth() {
        if (jsxContentBraceDepth() < LIVE_DEPTH_MASK) setTopFrame(topFrame() + 1);
    }

    private void decrementJsxContentBraceDepth() {
        if (jsxContentBraceDepth() > 0) setTopFrame(topFrame() - 1);
    }

    private void incrementJsxContentChildCount() {
        if (jsxContentChildCount() < LIVE_COUNT_MASK) setTopFrame(topFrame() + LIVE_ONE_CHILD);
    }

    private void decrementJsxContentChildCount() {
        if (jsxContentChildCount() > 0) setTopFrame(topFrame() - LIVE_ONE_CHILD);
    }

    // Directly between tags: the top frame is a children region AND no
    // `{child expr}` brace is open inside it. Only here do tag opens and closes
    // belong to the region's child count — inside a child brace the same tokens
    // belong to the nested expression. Without the depth half, a stray `</li>`
    // in a `{child}` brace would decrement the ENCLOSING region's count and
    // could pop a frame that is still live, taking the outer element with it.
    //
    // Not the same predicate as a bare topIsJsxContent(), which asks only
    // whether a children region is on top, at any brace depth — that is the
    // right question for maintaining the depth itself.
    private boolean directlyInJsxChildren() {
        return topIsJsxContent() && jsxContentBraceDepth() == 0;
    }

    // --- JSX element lifecycle ---------------------------------------------
    // One JSX_CONTENT frame covers a run of consecutively nested unbraced
    // elements, its child count saying how many are open. These four methods
    // are the only writers of that scheme; keeping them together is the point,
    // because the count is the subtlest thing in this file.

    // `>` ends an opening tag. Consecutive unbraced nesting shares one frame
    // via the child count; a fresh frame is pushed only when the enclosing
    // context is not itself a depth-0 children region (top level, attr/child
    // braces, interpolation).
    private void enterJsxChildren() {
        if (directlyInJsxChildren()) {
            incrementJsxContentChildCount();
        } else {
            pushJsxContentFrame();
        }
        yybegin(JSX_CHILDREN);
    }

    // `/>` ends a self-closing element: back to wherever the element appeared —
    // the children region of an enclosing element, or expression context.
    private void leaveSelfClosingElement() {
        if (directlyInJsxChildren()) {
            yybegin(JSX_CHILDREN);
        } else {
            yybegin(YYINITIAL);
        }
    }

    // The `>` of `</name >`: one fewer open element. Popping the frame means the
    // outermost element of this children region closed — the push invariant
    // guarantees the frame below is never a depth-0 children region — so we are
    // back in expression context.
    private void leaveClosedElement() {
        if (directlyInJsxChildren()) {
            decrementJsxContentChildCount();
            if (jsxContentChildCount() == 0) {
                popFrame();
                yybegin(YYINITIAL);
            } else {
                yybegin(JSX_CHILDREN);
            }
        } else {
            yybegin(YYINITIAL);
        }
    }

    // Unclosed-tag rescue: pops only an abandoned children frame, never a live
    // `${...}` one.
    private void dropAbandonedChildrenFrame() {
        if (directlyInJsxChildren()) popFrame();
        yybegin(YYINITIAL);
    }

    /** One live frame in the packed byte layout, each counter clamped to fit. */
    private int lowerFrame(int frame) {
        int kind = frame >>> LIVE_KIND_SHIFT;
        int depth = frame & LIVE_DEPTH_MASK;
        if (kind == FRAME_KIND_TEMPLATE) {
            int selector = (frame >>> LIVE_TEMPLATE_RETURN_SHIFT) & TEMPLATE_RETURN_MASK;
            return (kind << FRAME_KIND_SHIFT)
                | (selector << TEMPLATE_RETURN_SHIFT)
                | Math.min(depth, TEMPLATE_DEPTH_MASK);
        }
        if (kind == FRAME_KIND_JSX_CONTENT) {
            int count = (frame >>> LIVE_COUNT_SHIFT) & LIVE_COUNT_MASK;
            return (kind << FRAME_KIND_SHIFT)
                | (Math.min(count, JSX_CONTENT_COUNT_MASK) << JSX_CONTENT_COUNT_SHIFT)
                | Math.min(depth, JSX_CONTENT_DEPTH_MASK);
        }
        return (kind << FRAME_KIND_SHIFT) | Math.min(depth, FRAME_DEPTH_MASK);
    }

    /** Inverse of lowerFrame for a frame that fit; a clamped one raises differently. */
    private int raiseFrame(int packedFrame) {
        int kind = (packedFrame & CONTEXT_FRAME_MASK) >>> FRAME_KIND_SHIFT;
        if (kind == FRAME_KIND_TEMPLATE) {
            int selector = (packedFrame >>> TEMPLATE_RETURN_SHIFT) & TEMPLATE_RETURN_MASK;
            return (kind << LIVE_KIND_SHIFT)
                | (selector << LIVE_TEMPLATE_RETURN_SHIFT)
                | (packedFrame & TEMPLATE_DEPTH_MASK);
        }
        if (kind == FRAME_KIND_JSX_CONTENT) {
            int count = (packedFrame >>> JSX_CONTENT_COUNT_SHIFT) & JSX_CONTENT_COUNT_MASK;
            return (kind << LIVE_KIND_SHIFT)
                | (count << LIVE_COUNT_SHIFT)
                | (packedFrame & JSX_CONTENT_DEPTH_MASK);
        }
        return (kind << LIVE_KIND_SHIFT) | (packedFrame & FRAME_DEPTH_MASK);
    }

    private int packContextStack() {
        int packed = 0;
        int packable = Math.min(frameCount, PACKED_FRAMES);
        for (int i = 0; i < packable; i++) {
            int lowered = lowerFrame(frames[frameCount - 1 - i]) & CONTEXT_FRAME_MASK;
            packed |= lowered << (i * CONTEXT_FRAME_BITS);
        }
        return packed;
    }

    private void unpackContextStackInto(int packedStack) {
        frameCount = 0;
        // Outermost slot first, so the innermost frame ends up on top. A zero
        // byte is an empty slot; live frames are contiguous from the low byte.
        for (int i = PACKED_FRAMES - 1; i >= 0; i--) {
            int slot = (packedStack >>> (i * CONTEXT_FRAME_BITS)) & CONTEXT_FRAME_MASK;
            if (slot != 0) pushLiveFrame(raiseFrame(slot));
        }
    }

    /**
     * Whether the packed restart int describes the frame stack exactly. False
     * when the stack is deeper than PACKED_FRAMES, when a counter had to be
     * clamped, or when a frame lowered to an all-zero byte.
     *
     * Deliberately the round trip of the whole STACK, not of each frame in
     * isolation. Two things can be lost, and only one of them is visible per
     * frame: a clamped counter (which a depth test would miss) and a frame that
     * lowers to 0x00, which unpackContextStackInto reads as an empty slot and
     * drops — even though its bits round-trip perfectly. Today no live frame
     * can lower to zero (a TEMPLATE or JSX_ATTR frame is popped in the same
     * action that takes its depth to 0, and the other kinds carry a kind bit),
     * but that is an invariant of the rules, not of this method, and if it ever
     * broke the packed state would read as 0 — which the platform treats as
     * restartable, the one thing the design must never allow.
     */
    public boolean isRestartStateExact() {
        // Never actually false today — a nested comment is consumed inside a
        // single advance(), so the depth is zero at every token boundary — but
        // the clamp above is real, so the predicate accounts for it rather than
        // relying on that.
        if (commentDepth > COMMENT_DEPTH_MASK) return false;
        if (frameCount > PACKED_FRAMES) return false;
        int packed = packContextStack();
        for (int i = 0; i < frameCount; i++) {
            int slot = (packed >>> (i * CONTEXT_FRAME_BITS)) & CONTEXT_FRAME_MASK;
            if (slot == 0) return false;
            if (raiseFrame(slot) != frames[frameCount - 1 - i]) return false;
        }
        return true;
    }

    private boolean isSignificant(IElementType type) {
        return type != TokenType.WHITE_SPACE &&
               type != ReScriptTypes.LINE_COMMENT &&
               type != ReScriptTypes.BLOCK_COMMENT;
    }

    private IElementType track(IElementType type) {
        if (isSignificant(type)) {
            prevIsExprEnd = isExpressionEnd(type);
            sawLineBreak = false;
        }
        return type;
    }

    // For a token that ends a value but whose type alone can't say so: the
    // `>` finishing a closing tag (JSX_GT is also an opening tag's `>`,
    // which must NOT count — see isExpressionEnd).
    private IElementType trackExprEnd(IElementType type) {
        prevIsExprEnd = true;
        sawLineBreak = false;
        return type;
    }

    // A newline can also live INSIDE a significant token — string and template
    // content, and a char literal holding a raw newline in its content or in
    // an x/o/u escape tail — but a break inside the previous token is not a
    // break to bsc (`"a` NEWLINE `b" <c` is a comparison), so those interiors
    // deliberately do not report here and track() clears the flag when the
    // token completes. Every other newline is passed over, so noticing it
    // here covers every path: whitespace runs in each state, and
    // block-comment interiors.
    private void noteLineBreak(int start, int end) {
        for (int i = start; i < end; i++) {
            // LF only. bsc does not treat a lone CR as a break (`a` CR `<b` is
            // a comparison), and a CRLF run contains the LF anyway.
            if (zzBuffer.charAt(i) == '\n') {
                sawLineBreak = true;
                return;
            }
        }
    }

    private IElementType whiteSpace() {
        noteLineBreak(zzStartRead, zzMarkedPos);
        return TokenType.WHITE_SPACE;
    }

    /**
     * An "expression end" is a token that can terminate a value-producing
     * expression: identifiers, literals, closing delimiters, and the end of
     * a completed JSX element (an element is a value). The closing-tag `>`
     * is the one expression end this type check cannot express — JSX_GT also
     * ends opening tags — so the JSX_CLOSE_TAG rule marks it itself.
     */
    private static boolean isExpressionEnd(IElementType type) {
        return type == ReScriptTypes.LIDENT       ||   // x
               type == ReScriptTypes.UIDENT       ||   // Foo
               type == ReScriptTypes.INT          ||   // 10
               type == ReScriptTypes.FLOAT        ||   // 3.0
               type == ReScriptTypes.BIGINT       ||   // 1n
               type == ReScriptTypes.CHAR         ||   // 'a'
               type == ReScriptTypes.TRUE         ||   // true
               type == ReScriptTypes.FALSE        ||   // false
               type == ReScriptTypes.RPAREN       ||   // foo()
               type == ReScriptTypes.RBRACKET     ||   // arr[0]
               type == ReScriptTypes.RBRACE       ||   // {x}
               type == ReScriptTypes.STRING_END   ||   // "s"
               type == ReScriptTypes.TEMPLATE_END ||   // `t`
               type == ReScriptTypes.REGEX        ||   // /p/g
               type == ReScriptTypes.JSX_SLASH_GT;     // <A />
    }

    /**
     * Determines whether the next `/` should start a regex literal or be division.
     *
     * If the previous significant token is an "expression-end" (something that
     * produces a value), then `/` is division. Otherwise, `/` starts a regex.
     *
     * Division examples (previous token is expression-end):
     *   x / y          — LIDENT `/` → division
     *   Foo / bar      — UIDENT `/` → division
     *   10 / 2         — INT `/` → division
     *   3.0 / 2.0      — FLOAT `/` → division
     *   arr[0] / 2     — RBRACKET `/` → division
     *   foo() / bar    — RPAREN `/` → division
     *   {x} / y        — RBRACE `/` → division (blocks/records are expressions)
     *   "s" / x        — STRING_END `/` → division
     *   `t` / x        — TEMPLATE_END `/` → division
     *   /p/ / x        — REGEX `/` → division (a completed literal is a value)
     *   1n / 2n        — BIGINT `/` → division
     *   true / x       — TRUE/FALSE `/` → division
     *
     * Regex examples (previous token is NOT expression-end):
     *   let re = /p/   — EQ `/` → regex
     *   foo(/p/)       — LPAREN `/` → regex
     *   [/a/, /b/]     — LBRACKET or COMMA `/` → regex
     *   x => /p/       — FAT_ARROW `/` → regex
     *   a && /p/       — AMPAMP `/` → regex
     *   start of file  — no previous token → regex
     *
     * STRING_END / TEMPLATE_END / RBRACE matter for JSX: a self-closing element
     * whose last attribute is a quoted literal (`<B c="d" />`) ends with
     * `"`/`` ` `` immediately before the ` />`, and a JSX-element attribute value
     * (`icon={<B ... />}`) ends with `}` immediately before the outer ` />`.
     * Treating that `/` as a regex start would make the regex literal swallow the
     * `/>` (and any following `}`), leaving the enclosing braces/JSX expression
     * unbalanced — e.g. the nested shape `<A b={<C d={<E f="g" />} />} />`.
     */
    private boolean isStartRegexSlash() {
        return !prevIsExprEnd;
    }

    private int packRestartState(
            int lexicalState, int commentDepth, boolean prevIsExprEnd,
            boolean sawLineBreak, int contextStack) {
        // Guards the halving that freed bit 4. Both halves matter: an odd id
        // would lose its low bit, and a 17th state would not fit at all.
        // The tripwire is the test suite: this runs per token, so adding a
        // %state reddens every lexer test. In the IDE the adapter catches it
        // and degrades the file to BAD_CHARACTER plus a log warning.
        if ((lexicalState & 1) != 0 || (lexicalState >>> 1) > LEXICAL_STATE_MASK) {
            throw new IllegalStateException(
                "lexical state " + lexicalState + " does not fit the 4-bit restart field"
                    + " — rework the packed layout before adding states");
        }
        int packedLexicalState = (lexicalState >>> 1) & LEXICAL_STATE_MASK;
        int packedSawLineBreak = sawLineBreak ? SAW_LINE_BREAK_MASK : 0;
        int packedCommentDepth =
            Math.min(commentDepth, COMMENT_DEPTH_MASK) << COMMENT_DEPTH_SHIFT;
        int packedPrevIsExprEnd = prevIsExprEnd ? PREV_IS_EXPR_END_MASK : 0;
        int packedContextStack = (contextStack & CONTEXT_STACK_MASK) << CONTEXT_STACK_SHIFT;
        return packedLexicalState | packedSawLineBreak | packedCommentDepth
            | packedPrevIsExprEnd | packedContextStack;
    }

    private int unpackLexicalState(int packedState) {
        return (packedState & LEXICAL_STATE_MASK) << 1;
    }

    private boolean unpackSawLineBreak(int packedState) {
        return (packedState & SAW_LINE_BREAK_MASK) != 0;
    }

    private int unpackCommentDepth(int packedState) {
        return (packedState >>> COMMENT_DEPTH_SHIFT) & COMMENT_DEPTH_MASK;
    }

    private boolean unpackPrevIsExprEnd(int packedState) {
        return (packedState & PREV_IS_EXPR_END_MASK) != 0;
    }

    private int unpackContextStack(int packedState) {
        return (packedState >>> CONTEXT_STACK_SHIFT) & CONTEXT_STACK_MASK;
    }

    public int getPackedRestartState() {
        return packRestartState(
            zzLexicalState, commentDepth, prevIsExprEnd, sawLineBreak, packContextStack());
    }

    public void resetWithPackedRestartState(CharSequence buffer, int start, int end, int packedState) {
        prevIsExprEnd = unpackPrevIsExprEnd(packedState);
        // Never live across a restart (see the field's declaration); reset so
        // that is stated in code rather than merely true.
        blockCommentReturn = YYINITIAL;
        sawLineBreak = unpackSawLineBreak(packedState);
        commentDepth = unpackCommentDepth(packedState);
        unpackContextStackInto(unpackContextStack(packedState));
        reset(buffer, start, end, unpackLexicalState(packedState));
    }

%}

%state REGEX
%state IN_STRING
%state IN_TEMPLATE
%state IN_BLOCK_COMMENT
%state JSX_TAG
%state IN_TAG_STRING
%state IN_TAG_TEMPLATE
%state JSX_CHILDREN
%state JSX_CLOSE_TAG
%state IN_CHILD_STRING
%state IN_CHILD_TEMPLATE

WHITE_SPACE = [ \t\n\r]+
LINE_COMMENT = "//" [^\n]*
// The apostrophe is an identifier TAIL character in ReScript (x', y'z, let'),
// never a head, so a scan starting at ' can only be a char literal or TICK.
// Keyword rules survive the widening because longest match prefers the wider
// identifier: `let'` is a 4-char LIDENT, not LET + TICK — which is bsc's
// reading too.
IDENT_TAIL = [a-zA-Z0-9_']
LOWER_IDENT = [a-z_]{IDENT_TAIL}*
// Hyphenated web-component names (model-viewer, data-testid). Lowercase first
// segment only: uppercase names never hyphenate, and a segment cannot start
// with a digit.
JSX_HYPHEN_IDENT = [a-z_]{IDENT_TAIL}* ("-" [a-zA-Z_]{IDENT_TAIL}*)+
UPPER_IDENT = [A-Z]{IDENT_TAIL}*
// What may follow the keyword on a declaration-shaped line. The shape — not the
// keyword — is what decides: `<A b=` NEWLINE `module(M) />` is legal, a first-class
// module being an unbraced value, so `module(` must not fire where `module M = …` must.
JSX_DECL_RESCUE = ("let"|"and") [ \t]+ [a-z_({]
                | ("type"|"external") [ \t]+ [a-z_]
                | "module" [ \t]+ ("type" [ \t]+)? [A-Z]
                | ("open"|"include"|"exception") [ \t]+ [A-Z]
                | "@" | "%%"
HEX_INT = 0[xX][0-9a-fA-F][0-9a-fA-F_]*
OCT_INT = 0[oO][0-7][0-7_]*
BIN_INT = 0[bB][01][01_]*
BIGINT = [0-9][0-9_]*n
INT = [0-9][0-9_]*
FLOAT = [0-9][0-9_]* "." [0-9][0-9_]* ([eE][+-]?[0-9][0-9_]*)?
// Width-based and alphabet-agnostic, hex-checked only inside \u{…} — that is
// bsc's scanner. The short fixed forms ('\x4', '\o1', 1-2 digit
// decimals) are legal only at EOF or before a comment; the 0..N widths here
// deliberately over-accept them mid-file and bsc reports them. The x/o/u
// tails may contain newlines (bsc accepts '\x<NL>4'); the bare catch-all, the
// braced-u form and the decimal form may not ('\<NL>', '\u{<NL>41}' and
// '\1<NL>23' are all rejected), so those alphabets keep the exclusion.
CHAR_ESCAPE = \\ ( u\{ [0-9a-fA-F]* \}
                 | x [^]{0,2}
                 | o [^]{0,3}
                 | u [^]{0,4}
                 | [0-9]{1,3}
                 | [^\r\n] )
// The content class includes the apostrophe (''' is a legal char) and the
// newline (a raw newline is legal char content). It cannot run away: the
// content slot is exactly one character, so an unterminated ' fails back to
// TICK after mis-joining at most a few characters.
CHAR = ' ( [^\\] | {CHAR_ESCAPE} ) '

%%

// Rules that behave differently in expression context vs JSX children live in
// this YYINITIAL-only block (children has its own versions in <JSX_CHILDREN>);
// everything the two states lex identically sits in the shared block below.
// Invariant: no rule here may have a same-length competitor in the shared
// block — first-match-wins would silently prefer whichever comes first.
<YYINITIAL> {
    "/*"                { beginBlockComment(); }
    \"                  { yybegin(IN_STRING); return track(ReScriptTypes.STRING_START); }
    `                   { yybegin(IN_TEMPLATE); return track(ReScriptTypes.TEMPLATE_START); }

    // Closing tag in expression position: mid-edit recovery for text like
    // `<div> {x </div>`. After an expression end this is `a < /re/`
    // territory instead — push the slash back and let the regex rule decide.
    //
    // Deliberately NOT given the line-break exemption that `<` gets below: a
    // line-opening `</` only occurs in input bsc rejects outright, so there is
    // no correct reading to match, and the fallback here already stays local
    // (the regex attempt dies at the line end rather than eating the file).
    "</"                { if (!prevIsExprEnd) {
                              yybegin(JSX_CLOSE_TAG);
                              return track(ReScriptTypes.JSX_LT_SLASH);
                          } else {
                              yypushback(1);
                              return track(ReScriptTypes.LT);
                          }
                        }

    // JSX vs comparison disambiguation: `<` in expression position starts a
    // tag (`let x = <div />`); `<` after an expression-end token is comparison
    // or a type parameter list (`a < b`, `list<int>`). The lookahead keeps
    // bare operator soup like `== != < >` out of JSX mode: a tag's `<` is
    // always glued to a name, `>`, or `/`.
    //
    // A `<` separated from the previous token by a line break is a tag even
    // after an expression end, which is how an element in statement position
    // parses — the `@react.component let make` body returns one right after a
    // `let` ending in `}`. bsc draws the line in the same place.
    "<" / [A-Za-z_>/]   { if (!prevIsExprEnd || sawLineBreak) {
                              yybegin(JSX_TAG);
                              return track(ReScriptTypes.JSX_LT);
                          } else {
                              return track(ReScriptTypes.LT);
                          }
                        }
    "<"                 { return track(ReScriptTypes.LT); }

    // Regex vs division disambiguation: check previous token
    "/"                 { if (isStartRegexSlash()) {
                              yybegin(REGEX);
                              yypushback(1); // un-eat the /
                          } else {
                              return track(ReScriptTypes.SLASH);
                          }
                        }

    "{"                 {
                            if (topIsJsxContent()) incrementJsxContentBraceDepth();
                            else if (hasFrame()) incrementTopFrameDepth();
                            return track(ReScriptTypes.LBRACE);
                        }
}

// Between an opening tag's `>` and the matching `</`. Children are ordinary
// ReScript atoms (idents, literals, `{expr}`, nested elements), so this state
// shares YYINITIAL's rule bulk below and only overrides the JSX-specific and
// context-entering rules. This block sits BEFORE the shared block so that on
// same-length ties JFlex's first-match-wins picks these overrides; today no
// same-length competitor exists in the shared block, but the ordering keeps
// that from becoming a trap.
<JSX_CHILDREN> {
    "</"                { yybegin(JSX_CLOSE_TAG); return track(ReScriptTypes.JSX_LT_SLASH); }
    "<" / [A-Za-z_>/]   { yybegin(JSX_TAG); return track(ReScriptTypes.JSX_LT); }
    // Braced child expression: resume normal lexing until the matching `}`
    // returns to this children region (tracked in the JSX_CONTENT frame).
    "{"                 {
                            if (topIsJsxContent()) incrementJsxContentBraceDepth();
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.LBRACE);
                        }
    // Longest match beats the `/` rule below.
    "/*"                { beginBlockComment(); }
    // Always SLASH — a deliberate divergence: bsc accepts a bare regex child
    // (`<div> /a/ </div>`), but letting `/` open one here would let it swallow
    // the `</` this state exists to see. The braced form `{/a/}` lexes correctly.
    "/"                 { return track(ReScriptTypes.SLASH); }
    // A `<` not glued to a name/`>`/`/` is a mid-edit stray, not a tag.
    "<"                 { return track(ReScriptTypes.LT); }
    \"                  { yybegin(IN_CHILD_STRING); return track(ReScriptTypes.STRING_START); }
    `                   { yybegin(IN_CHILD_TEMPLATE); return track(ReScriptTypes.TEMPLATE_START); }
}

// Token bulk shared by expression context and JSX children: keywords,
// identifiers, literals, operators, punctuation. Children of an element are
// ordinary ReScript atoms, so both states lex these identically.
<YYINITIAL, JSX_CHILDREN> {
    {WHITE_SPACE}       { return whiteSpace(); }
    {LINE_COMMENT}      { return track(ReScriptTypes.LINE_COMMENT); }

    "let"               { return track(ReScriptTypes.LET); }
    "type"              { return track(ReScriptTypes.TYPE); }
    "module"            { return track(ReScriptTypes.MODULE); }
    "open"              { return track(ReScriptTypes.OPEN); }
    "include"           { return track(ReScriptTypes.INCLUDE); }
    "external"          { return track(ReScriptTypes.EXTERNAL); }
    "exception"         { return track(ReScriptTypes.EXCEPTION); }
    "rec"               { return track(ReScriptTypes.REC); }
    "private"           { return track(ReScriptTypes.PRIVATE); }
    "true"              { return track(ReScriptTypes.TRUE); }
    "false"             { return track(ReScriptTypes.FALSE); }
    "switch"            { return track(ReScriptTypes.SWITCH); }
    "if"                { return track(ReScriptTypes.IF); }
    "else"              { return track(ReScriptTypes.ELSE); }
    "async"             { return track(ReScriptTypes.ASYNC); }
    "await"             { return track(ReScriptTypes.AWAIT); }
    "try"               { return track(ReScriptTypes.TRY); }
    "catch"             { return track(ReScriptTypes.CATCH); }
    "while"             { return track(ReScriptTypes.WHILE); }
    "for"               { return track(ReScriptTypes.FOR); }
    "and"               { return track(ReScriptTypes.AND); }
    "as"                { return track(ReScriptTypes.AS); }

    {FLOAT}             { return track(ReScriptTypes.FLOAT); }
    {HEX_INT}           { return track(ReScriptTypes.INT); }
    {OCT_INT}           { return track(ReScriptTypes.INT); }
    {BIN_INT}           { return track(ReScriptTypes.INT); }
    {BIGINT}            { return track(ReScriptTypes.BIGINT); }
    {INT}               { return track(ReScriptTypes.INT); }

    "_"                 { return track(ReScriptTypes.UNDERSCORE); }
    {LOWER_IDENT}       { return track(ReScriptTypes.LIDENT); }
    {UPPER_IDENT}       { return track(ReScriptTypes.UIDENT); }

    "&&&"               { return track(ReScriptTypes.AMPAMPAMP); }
    "&&"                { return track(ReScriptTypes.AMPAMP); }
    "|||"               { return track(ReScriptTypes.PIPEPIPEPIPE); }
    "||"                { return track(ReScriptTypes.PIPEPIPE); }
    "^^^"               { return track(ReScriptTypes.CARETCARETCARET); }
    "~~~"               { return track(ReScriptTypes.TILDETILDETILDE); }
    "<<"                { return track(ReScriptTypes.SHIFT_LEFT); }
    ">>>"               { return track(ReScriptTypes.SHIFT_RIGHT_UNSIGNED); }
    ">>"                { return track(ReScriptTypes.SHIFT_RIGHT); }
    "==="               { return track(ReScriptTypes.EQEQEQ); }
    "=="                { return track(ReScriptTypes.EQEQ); }
    "!=="               { return track(ReScriptTypes.BANGEQEQ); }
    "!="                { return track(ReScriptTypes.BANGEQ); }
    "<="                { return track(ReScriptTypes.LTEQ); }
    ">="                { return track(ReScriptTypes.GTEQ); }
    "->"                { return track(ReScriptTypes.ARROW); }
    "=>"                { return track(ReScriptTypes.FAT_ARROW); }
    "|>"                { return track(ReScriptTypes.PIPE_FORWARD); }
    "+."                { return track(ReScriptTypes.PLUS_DOT); }
    "-."                { return track(ReScriptTypes.MINUS_DOT); }
    "**"                { return track(ReScriptTypes.STARSTAR); }
    "*."                { return track(ReScriptTypes.STAR_DOT); }
    "/."                { return track(ReScriptTypes.SLASH_DOT); }
    "="                 { return track(ReScriptTypes.EQ); }
    "+"                 { return track(ReScriptTypes.PLUS); }
    "-"                 { return track(ReScriptTypes.MINUS); }
    "*"                 { return track(ReScriptTypes.STAR); }
    ">"                 { return track(ReScriptTypes.GT); }

    "("                 {
                            if (topIsJsxValue()) incrementTopFrameDepth();
                            return track(ReScriptTypes.LPAREN);
                        }
    ")"                 {
                            closeJsxValueDelimiter();
                            return track(ReScriptTypes.RPAREN);
                        }
    "}"                 {
                            if (topIsJsxContent()) {
                                // Closing a `{child expr}` brace (or a stray `}`
                                // directly between tags when the depth is 0).
                                if (jsxContentBraceDepth() > 0) {
                                    decrementJsxContentBraceDepth();
                                    if (jsxContentBraceDepth() == 0) yybegin(JSX_CHILDREN);
                                }
                                return track(ReScriptTypes.RBRACE);
                            }
                            if (hasFrame()) {
                                decrementTopFrameDepth();
                                if (topFrameDepth() == 0) {
                                    int kind = topFrameKind();
                                    int templateReturn = topTemplateReturnState();
                                    popFrame();
                                    if (kind == FRAME_KIND_TEMPLATE) {
                                        yybegin(templateReturn);
                                        return track(ReScriptTypes.TEMPLATE_INTERPOLATION_END);
                                    }
                                    // FRAME_KIND_JSX_ATTR or FRAME_KIND_JSX_VALUE:
                                    // `}` closes an attribute expression or an
                                    // unbraced value's region, back into the tag.
                                    yybegin(JSX_TAG);
                                    return track(ReScriptTypes.RBRACE);
                                }
                            }
                            return track(ReScriptTypes.RBRACE);
                        }
    "["                 {
                            if (topIsJsxValue()) incrementTopFrameDepth();
                            return track(ReScriptTypes.LBRACKET);
                        }
    "]"                 {
                            closeJsxValueDelimiter();
                            return track(ReScriptTypes.RBRACKET);
                        }
    ","                 { return track(ReScriptTypes.COMMA); }
    ";"                 { return track(ReScriptTypes.SEMICOLON); }
    ":>"                { return track(ReScriptTypes.COLONGT); }
    ":"                 { return track(ReScriptTypes.COLON); }
    "..."               { return track(ReScriptTypes.DOTDOTDOT); }
    ".."                { return track(ReScriptTypes.DOTDOT); }
    "."                 { return track(ReScriptTypes.DOT); }
    "@"                 { return track(ReScriptTypes.AT); }
    "~"                 { return track(ReScriptTypes.TILDE); }
    "|"                 { return track(ReScriptTypes.PIPE); }
    "!"                 { return track(ReScriptTypes.BANG); }
    "?"                 { return track(ReScriptTypes.QUESTION); }
    "#"                 { return track(ReScriptTypes.HASH); }
    {CHAR}              { return track(ReScriptTypes.CHAR); }
    "'"                 { return track(ReScriptTypes.TICK); }
    "%%"                { return track(ReScriptTypes.PCT_PCT); }
    "%"                 { return track(ReScriptTypes.PCT); }
}

// Inside an opening tag: `<name attr=... />` or `<name ...>`. Tag and
// attribute names deliberately stay LIDENT/UIDENT — structural roles are the
// parser's job (follow-up ticket); this state only owns the delimiters.
<JSX_TAG> {
    // Unclosed-tag rescue: a declaration-shaped next line ends the tag mid-edit.
    // The lookahead is unconsumed, so the keyword re-lexes in YYINITIAL. The
    // depth guard pops only an abandoned children frame, never a live `${...}` one.
    [\r\n]+ [ \t]* / {JSX_DECL_RESCUE} { dropAbandonedChildrenFrame(); return whiteSpace(); }
    // ONE token, deliberately not LIDENT MINUS LIDENT: `-` stays unlexable in
    // tag states, so a non-value like `neg=-1` cannot form by construction.
    {JSX_HYPHEN_IDENT}  { return track(ReScriptTypes.LIDENT); }
    {WHITE_SPACE}       { return whiteSpace(); }
    {FLOAT}             { return track(ReScriptTypes.FLOAT); }
    {HEX_INT}           { return track(ReScriptTypes.INT); }
    {OCT_INT}           { return track(ReScriptTypes.INT); }
    {BIN_INT}           { return track(ReScriptTypes.INT); }
    {BIGINT}            { return track(ReScriptTypes.BIGINT); }
    {INT}               { return track(ReScriptTypes.INT); }
    {LOWER_IDENT}       { return track(ReScriptTypes.LIDENT); }
    {UPPER_IDENT}       { return track(ReScriptTypes.UIDENT); }
    {CHAR}              { return track(ReScriptTypes.CHAR); }
    "."                 { return track(ReScriptTypes.DOT); }
    "="                 { return track(ReScriptTypes.EQ); }
    "?"                 { return track(ReScriptTypes.QUESTION); }
    "#"                 { return track(ReScriptTypes.HASH); }
    // `b=%raw("x")` — an extension is a legal unbraced value. Safe to lex here
    // for the same reason `(` is: it cannot begin anything the tag rejects.
    "%"                 { return track(ReScriptTypes.PCT); }
    // Comments are legal at every inter-token position in a tag. The parser
    // skips them for free — they are in getCommentTokens() — so nothing in the
    // grammar has to admit them.
    {LINE_COMMENT}      { return track(ReScriptTypes.LINE_COMMENT); }
    "/*"                { beginBlockComment(); }
    \"                  { yybegin(IN_TAG_STRING); return track(ReScriptTypes.STRING_START); }
    `                   { yybegin(IN_TAG_TEMPLATE); return track(ReScriptTypes.TEMPLATE_START); }
    // Unbraced applied or container value: `b=f(x)`, `b=Some(1)`, `b=#tag(x)`,
    // `b=[1, 2]`, `b=(a, b)`, `b=x[0]`. ReScript allows any *primary*
    // expression here, so the interior is ordinary expression text — same
    // treatment as `{` below; the closer that brings the depth to zero returns
    // to this tag.
    //
    // This does NOT weaken the rule that `-` is unlexable in a tag: the
    // exclusion guards where a value STARTS, so `b=-1` still cannot form, while
    // `b=Some(-1)` — which the compiler accepts — lexes inside the region.
    "("                 {
                            pushJsxValueFrame();
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.LPAREN);
                        }
    "["                 {
                            pushJsxValueFrame();
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.LBRACKET);
                        }
    // Attribute expression (`b={expr}`) or spread (`{...props}`): resume
    // normal lexing until the matching `}` returns to this tag.
    "{"                 {
                            pushJsxAttrFrame();
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.LBRACE);
                        }
    // Opening tag ends: enter the children region. Consecutive unbraced
    // nesting shares one JSX_CONTENT frame via its child count; a fresh
    // frame is pushed only when the enclosing context is not itself a
    // depth-0 children region (top level, attr/child braces, interpolation).
    ">"                 { enterJsxChildren(); return track(ReScriptTypes.JSX_GT); }
    // Self-closing element: back to wherever the element appeared — the
    // children region of an enclosing element, or expression context.
    "/>"                { leaveSelfClosingElement(); return track(ReScriptTypes.JSX_SLASH_GT); }
}

// Inside `</name >`. Only names and the closing `>` belong here; a newline
// bails back to children so mid-edit text on the next line lexes normally
// (mirrors the REGEX end-of-line rescue).
<JSX_CLOSE_TAG> {
    {JSX_HYPHEN_IDENT}  { return track(ReScriptTypes.LIDENT); }
    {LINE_COMMENT}      { return track(ReScriptTypes.LINE_COMMENT); }
    "/*"                { beginBlockComment(); }
    [ \t]+              { return TokenType.WHITE_SPACE; }
    // Mid-edit bail, guarded exactly like JSX_TAG's. An unguarded bail (any
    // newline) breaks the legal `</A` NEWLINE `>`: it drops to children and
    // lexes the `>` as a comparison, leaving the tag unclosed.
    [\r\n]+ [ \t]* / {JSX_DECL_RESCUE} {
                            yybegin(JSX_CHILDREN);
                            return whiteSpace();
                        }
    [\r\n]+             { return whiteSpace(); }
    {LOWER_IDENT}       { return track(ReScriptTypes.LIDENT); }
    {UPPER_IDENT}       { return track(ReScriptTypes.UIDENT); }
    "."                 { return track(ReScriptTypes.DOT); }
    // Closing tag done: one fewer open element. Popping the frame means the
    // outermost element of this children region closed, so we are back in
    // expression context (the push invariant guarantees the frame below is
    // never a depth-0 children region). The finished element is a value, so
    // this `>` is an expression end (trackExprEnd, not track).
    ">"                 { leaveClosedElement(); return trackExprEnd(ReScriptTypes.JSX_GT); }
}

// Regex literal state: match /pattern/flags as a single token
<REGEX> {
    "/" ( [^/\\\n] | "\\". )* "/" [dgimsuvy]* {
        yybegin(YYINITIAL);
        return track(ReScriptTypes.REGEX);
    }

    // Failed to match a complete regex — fall back to SLASH
    "/" {
        yybegin(YYINITIAL);
        return track(ReScriptTypes.SLASH);
    }
}

// Double-quoted string states. IN_TAG_STRING is the same string, opened as a
// JSX attribute value (`c="d"`) — only the exit state differs.
<IN_STRING> {
    \"                  { yybegin(YYINITIAL); return track(ReScriptTypes.STRING_END); }
}
<IN_TAG_STRING> {
    \"                  { yybegin(JSX_TAG); return track(ReScriptTypes.STRING_END); }
}
<IN_CHILD_STRING> {
    \"                  { yybegin(JSX_CHILDREN); return track(ReScriptTypes.STRING_END); }
}
<IN_STRING, IN_TAG_STRING, IN_CHILD_STRING> {
    \\[\\\"ntbr0]       { return track(ReScriptTypes.STRING_ESCAPE); }
    \\x[0-9a-fA-F]{2}  { return track(ReScriptTypes.STRING_ESCAPE); }
    \\.                 { return track(ReScriptTypes.STRING_ESCAPE); }
    // Consume ordinary string content; stop before `"` closes the string or `\` starts an escape.
    [^\"\\]+            { return ReScriptTypes.STRING_CONTENT; }
}

// Backtick template string states. IN_TAG_TEMPLATE is a template opened as a
// JSX attribute value (`c=`d``) — only the exit state differs.
<IN_TEMPLATE> {
    // A raw backtick closes the current template.
    `                   { yybegin(YYINITIAL); return track(ReScriptTypes.TEMPLATE_END); }

    // `${` starts an interpolation: resume normal ReScript lexing until its
    // matching `}` returns to this template state (via the frame's selector).
    "${"                {
                            pushTemplateFrame(TEMPLATE_RETURN_TOP);
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.TEMPLATE_INTERPOLATION_START);
                        }
}
<IN_TAG_TEMPLATE> {
    `                   { yybegin(JSX_TAG); return track(ReScriptTypes.TEMPLATE_END); }
    "${"                {
                            pushTemplateFrame(TEMPLATE_RETURN_TAG);
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.TEMPLATE_INTERPOLATION_START);
                        }
}
<IN_CHILD_TEMPLATE> {
    `                   { yybegin(JSX_CHILDREN); return track(ReScriptTypes.TEMPLATE_END); }
    "${"                {
                            pushTemplateFrame(TEMPLATE_RETURN_CHILD);
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.TEMPLATE_INTERPOLATION_START);
                        }
}
<IN_TEMPLATE, IN_TAG_TEMPLATE, IN_CHILD_TEMPLATE> {
    // Template content is one or more ordinary chars or escaped chars.
    // Ordinary chars exclude `$`, raw backtick, and backslash because those
    // start interpolation, end the template, or start an escaped char.
    ( [^$`\\] | "\\". )+ { return track(ReScriptTypes.TEMPLATE_CONTENT); }

    // A lone `$` is content; only `${` starts interpolation.
    "$"                 { return track(ReScriptTypes.TEMPLATE_CONTENT); }
}

// Nested block comment state: /* /* */ */
<IN_BLOCK_COMMENT> {
    // Full width, like the frame stack: the packed copy clamps (see
    // packRestartState), the live counter does not. A saturating counter here
    // loses the increment outright: `/* a /* b /* c /* d */ */ */ */` closes
    // one `*/` early and leaks the last one into the token stream.
    "/*"                { commentDepth++; }
    "*/"                { commentDepth--;
                          if (commentDepth == 0) {
                              yybegin(blockCommentReturn);
                              return track(ReScriptTypes.BLOCK_COMMENT);
                          } }
    [^]                 { noteLineBreak(zzStartRead, zzMarkedPos); }
}

[^]                     { return TokenType.BAD_CHARACTER; }
