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
    // restart context into bit fields inside that int (layout v2):
    //   bits 0..4    = JFlex lexical state
    //   bits 5..6    = block-comment nesting depth (saturating at 3)
    //   bit  7       = previous significant token is an "expression end"
    //   bits 8..31   = context stack: three 8-bit frames, low frame = innermost
    //
    // A context frame remembers why a `{` region was opened, so the matching
    // `}` can restore the right lexer state. Frame = 2-bit kind (high bits)
    // + 6-bit brace depth. Depth is >= 1 while a frame is live, so a live
    // frame's byte is never zero and 0x00 unambiguously means "empty slot".
    //
    // All depth counters saturate at their field maximum: pathological
    // nesting closes regions early, but full lexing and restarted lexing
    // agree on that (wrong) answer — which is the property incremental
    // lexing needs. Pushing onto a full stack drops the outermost frame:
    // same trade, mis-scoping stays local to the overflow point.
    private static final int LEXICAL_STATE_MASK = 0x1F;
    private static final int COMMENT_DEPTH_MASK = 0x3;
    private static final int CONTEXT_STACK_MASK = 0xFFFFFF;

    private static final int COMMENT_DEPTH_SHIFT = 5;
    private static final int PREV_IS_EXPR_END_SHIFT = 7;
    private static final int CONTEXT_STACK_SHIFT = 8;

    // Public so tests can ignore this bit when asserting zero-state restart
    // boundaries: the bit is set by ordinary tokens (any identifier or literal
    // sets it) and self-corrects on the first significant token after restart.
    public static final int PREV_IS_EXPR_END_MASK = 1 << PREV_IS_EXPR_END_SHIFT;

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
    private static final int JSX_CONTENT_ONE_CHILD = 1 << JSX_CONTENT_COUNT_SHIFT;

    // Whether the previous significant token is an "expression end" — drives
    // regex-vs-division disambiguation for `/` and JSX-vs-comparison for `<`.
    // Packed into the restart int (bit 7) so a restart in the middle of e.g.
    // `x / y / z` still knows the `/` follows a value and must be division,
    // not a regex start.
    private boolean prevIsExprEnd = false;
    private int commentDepth = 0;
    private int contextStack = 0;

    private boolean hasFrame() {
        return (contextStack & CONTEXT_FRAME_MASK) != 0;
    }

    private int topFrameKind() {
        return (contextStack & CONTEXT_FRAME_MASK) >>> FRAME_KIND_SHIFT;
    }

    // Depth of the top TEMPLATE or JSX_ATTR frame (JSX_CONTENT frames have a
    // split payload and their own accessors below).
    private int topFrameDepth() {
        int mask = topFrameKind() == FRAME_KIND_TEMPLATE ? TEMPLATE_DEPTH_MASK : FRAME_DEPTH_MASK;
        return contextStack & mask;
    }

    private int topTemplateReturnState() {
        int selector = (contextStack >>> TEMPLATE_RETURN_SHIFT) & TEMPLATE_RETURN_MASK;
        if (selector == TEMPLATE_RETURN_TAG) return IN_TAG_TEMPLATE;
        if (selector == TEMPLATE_RETURN_CHILD) return IN_CHILD_TEMPLATE;
        return IN_TEMPLATE;
    }

    private void pushFrame(int kind, int payload) {
        contextStack = ((contextStack << CONTEXT_FRAME_BITS) & CONTEXT_STACK_MASK)
            | (kind << FRAME_KIND_SHIFT) | (payload & FRAME_DEPTH_MASK);
    }

    private void popFrame() {
        contextStack >>>= CONTEXT_FRAME_BITS;
    }

    private void incrementTopFrameDepth() {
        int max = topFrameKind() == FRAME_KIND_TEMPLATE ? TEMPLATE_DEPTH_MASK : FRAME_DEPTH_MASK;
        if (topFrameDepth() < max) contextStack++;
    }

    private void decrementTopFrameDepth() {
        if (topFrameDepth() > 0) contextStack--;
    }

    private boolean topIsJsxContent() {
        return hasFrame() && topFrameKind() == FRAME_KIND_JSX_CONTENT;
    }

    private int jsxContentBraceDepth() {
        return contextStack & JSX_CONTENT_DEPTH_MASK;
    }

    private int jsxContentChildCount() {
        return (contextStack >>> JSX_CONTENT_COUNT_SHIFT) & JSX_CONTENT_COUNT_MASK;
    }

    private void incrementJsxContentBraceDepth() {
        if (jsxContentBraceDepth() < JSX_CONTENT_DEPTH_MASK) contextStack++;
    }

    private void decrementJsxContentBraceDepth() {
        if (jsxContentBraceDepth() > 0) contextStack--;
    }

    private void incrementJsxContentChildCount() {
        if (jsxContentChildCount() < JSX_CONTENT_COUNT_MASK) contextStack += JSX_CONTENT_ONE_CHILD;
    }

    private void decrementJsxContentChildCount() {
        if (jsxContentChildCount() > 0) contextStack -= JSX_CONTENT_ONE_CHILD;
    }

    private boolean isSignificant(IElementType type) {
        return type != TokenType.WHITE_SPACE &&
               type != ReScriptTypes.LINE_COMMENT &&
               type != ReScriptTypes.BLOCK_COMMENT;
    }

    private IElementType track(IElementType type) {
        if (isSignificant(type)) prevIsExprEnd = isExpressionEnd(type);
        return type;
    }

    // For a token that ends a value but whose type alone can't say so: the
    // `>` finishing a closing tag (JSX_GT is also an opening tag's `>`,
    // which must NOT count — see isExpressionEnd).
    private IElementType trackExprEnd(IElementType type) {
        prevIsExprEnd = true;
        return type;
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
               type == ReScriptTypes.TRUE         ||   // true
               type == ReScriptTypes.FALSE        ||   // false
               type == ReScriptTypes.RPAREN       ||   // foo()
               type == ReScriptTypes.RBRACKET     ||   // arr[0]
               type == ReScriptTypes.RBRACE       ||   // {x}
               type == ReScriptTypes.STRING_END   ||   // "s"
               type == ReScriptTypes.TEMPLATE_END ||   // `t`
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
            int lexicalState, int commentDepth, boolean prevIsExprEnd, int contextStack) {
        int packedLexicalState = lexicalState & LEXICAL_STATE_MASK;
        int packedCommentDepth = (commentDepth & COMMENT_DEPTH_MASK) << COMMENT_DEPTH_SHIFT;
        int packedPrevIsExprEnd = prevIsExprEnd ? PREV_IS_EXPR_END_MASK : 0;
        int packedContextStack = (contextStack & CONTEXT_STACK_MASK) << CONTEXT_STACK_SHIFT;
        return packedLexicalState | packedCommentDepth | packedPrevIsExprEnd | packedContextStack;
    }

    private int unpackLexicalState(int packedState) {
        return packedState & LEXICAL_STATE_MASK;
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
        return packRestartState(zzLexicalState, commentDepth, prevIsExprEnd, contextStack);
    }

    public void resetWithPackedRestartState(CharSequence buffer, int start, int end, int packedState) {
        prevIsExprEnd = unpackPrevIsExprEnd(packedState);
        commentDepth = unpackCommentDepth(packedState);
        contextStack = unpackContextStack(packedState);
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
LOWER_IDENT = [a-z_][a-zA-Z0-9_]*
UPPER_IDENT = [A-Z][a-zA-Z0-9_]*
HEX_INT = 0[xX][0-9a-fA-F][0-9a-fA-F_]*
OCT_INT = 0[oO][0-7][0-7_]*
BIN_INT = 0[bB][01][01_]*
BIGINT = [0-9][0-9_]*n
INT = [0-9][0-9_]*
FLOAT = [0-9][0-9_]* "." [0-9][0-9_]* ([eE][+-]?[0-9][0-9_]*)?

%%

// Rules that behave differently in expression context vs JSX children live in
// this YYINITIAL-only block (children has its own versions in <JSX_CHILDREN>);
// everything the two states lex identically sits in the shared block below.
// Invariant: no rule here may have a same-length competitor in the shared
// block — first-match-wins would silently prefer whichever comes first.
<YYINITIAL> {
    "/*"                { commentDepth = 1; yybegin(IN_BLOCK_COMMENT); }
    \"                  { yybegin(IN_STRING); return track(ReScriptTypes.STRING_START); }
    `                   { yybegin(IN_TEMPLATE); return track(ReScriptTypes.TEMPLATE_START); }

    // Closing tag in expression position: mid-edit recovery for text like
    // `<div> {x </div>`. After an expression end this is `a < /re/`
    // territory instead — push the slash back and let the regex rule decide.
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
    "<" / [A-Za-z_>/]   { if (!prevIsExprEnd) {
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
    // `/` never starts a regex between tags.
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
    {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
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

    "("                 { return track(ReScriptTypes.LPAREN); }
    ")"                 { return track(ReScriptTypes.RPAREN); }
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
                                    // FRAME_KIND_JSX_ATTR: `}` closes an attribute
                                    // expression, back into the enclosing tag.
                                    yybegin(JSX_TAG);
                                    return track(ReScriptTypes.RBRACE);
                                }
                            }
                            return track(ReScriptTypes.RBRACE);
                        }
    "["                 { return track(ReScriptTypes.LBRACKET); }
    "]"                 { return track(ReScriptTypes.RBRACKET); }
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
    "'"                 { return track(ReScriptTypes.TICK); }
    "%%"                { return track(ReScriptTypes.PCT_PCT); }
    "%"                 { return track(ReScriptTypes.PCT); }
}

// Inside an opening tag: `<name attr=... />` or `<name ...>`. Tag and
// attribute names deliberately stay LIDENT/UIDENT — structural roles are the
// parser's job (follow-up ticket); this state only owns the delimiters.
<JSX_TAG> {
    {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
    {LOWER_IDENT}       { return track(ReScriptTypes.LIDENT); }
    {UPPER_IDENT}       { return track(ReScriptTypes.UIDENT); }
    "."                 { return track(ReScriptTypes.DOT); }
    "="                 { return track(ReScriptTypes.EQ); }
    "?"                 { return track(ReScriptTypes.QUESTION); }
    \"                  { yybegin(IN_TAG_STRING); return track(ReScriptTypes.STRING_START); }
    `                   { yybegin(IN_TAG_TEMPLATE); return track(ReScriptTypes.TEMPLATE_START); }
    // Attribute expression (`b={expr}`) or spread (`{...props}`): resume
    // normal lexing until the matching `}` returns to this tag.
    "{"                 {
                            pushFrame(FRAME_KIND_JSX_ATTR, 1);
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.LBRACE);
                        }
    // Opening tag ends: enter the children region. Consecutive unbraced
    // nesting shares one JSX_CONTENT frame via its child count; a fresh
    // frame is pushed only when the enclosing context is not itself a
    // depth-0 children region (top level, attr/child braces, interpolation).
    ">"                 {
                            if (topIsJsxContent() && jsxContentBraceDepth() == 0) {
                                incrementJsxContentChildCount();
                            } else {
                                pushFrame(FRAME_KIND_JSX_CONTENT, JSX_CONTENT_ONE_CHILD);
                            }
                            yybegin(JSX_CHILDREN);
                            return track(ReScriptTypes.JSX_GT);
                        }
    // Self-closing element: back to wherever the element appeared — the
    // children region of an enclosing element, or expression context.
    "/>"                {
                            if (topIsJsxContent() && jsxContentBraceDepth() == 0) {
                                yybegin(JSX_CHILDREN);
                            } else {
                                yybegin(YYINITIAL);
                            }
                            return track(ReScriptTypes.JSX_SLASH_GT);
                        }
}

// Inside `</name >`. Only names and the closing `>` belong here; a newline
// bails back to children so mid-edit text on the next line lexes normally
// (mirrors the REGEX end-of-line rescue).
<JSX_CLOSE_TAG> {
    [ \t]+              { return TokenType.WHITE_SPACE; }
    [\r\n]+             { yybegin(JSX_CHILDREN); return TokenType.WHITE_SPACE; }
    {LOWER_IDENT}       { return track(ReScriptTypes.LIDENT); }
    {UPPER_IDENT}       { return track(ReScriptTypes.UIDENT); }
    "."                 { return track(ReScriptTypes.DOT); }
    // Closing tag done: one fewer open element. Popping the frame means the
    // outermost element of this children region closed, so we are back in
    // expression context (the push invariant guarantees the frame below is
    // never a depth-0 children region). The finished element is a value, so
    // this `>` is an expression end (trackExprEnd, not track).
    ">"                 {
                            if (topIsJsxContent()) {
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
                            return trackExprEnd(ReScriptTypes.JSX_GT);
                        }
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
                            pushFrame(FRAME_KIND_TEMPLATE, (TEMPLATE_RETURN_TOP << TEMPLATE_RETURN_SHIFT) | 1);
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.TEMPLATE_INTERPOLATION_START);
                        }
}
<IN_TAG_TEMPLATE> {
    `                   { yybegin(JSX_TAG); return track(ReScriptTypes.TEMPLATE_END); }
    "${"                {
                            pushFrame(FRAME_KIND_TEMPLATE, (TEMPLATE_RETURN_TAG << TEMPLATE_RETURN_SHIFT) | 1);
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.TEMPLATE_INTERPOLATION_START);
                        }
}
<IN_CHILD_TEMPLATE> {
    `                   { yybegin(JSX_CHILDREN); return track(ReScriptTypes.TEMPLATE_END); }
    "${"                {
                            pushFrame(FRAME_KIND_TEMPLATE, (TEMPLATE_RETURN_CHILD << TEMPLATE_RETURN_SHIFT) | 1);
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
    // Saturate at the packed field's maximum so the in-memory depth is always
    // exactly what a restart would restore (see COMMENT_DEPTH_MASK).
    "/*"                { if (commentDepth < COMMENT_DEPTH_MASK) commentDepth++; }
    "*/"                { commentDepth--; if (commentDepth == 0) { yybegin(YYINITIAL); return track(ReScriptTypes.BLOCK_COMMENT); } }
    [^]                 { /* consume */ }
}

[^]                     { return TokenType.BAD_CHARACTER; }
