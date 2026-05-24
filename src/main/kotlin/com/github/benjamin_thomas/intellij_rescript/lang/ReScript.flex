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
    // restart context into bit fields inside that int:
    //   bits 0..4    = JFlex lexical state
    //   bits 5..11   = block-comment nesting depth
    //   bits 12..31  = packed template-interpolation context
    //
    // The interpolation context is a stack of 5-bit brace-depth frames. The
    // low frame is the current `${...}` depth; higher frames remember enclosing
    // interpolation depths when a nested template starts its own interpolation.
    private static final int LEXICAL_STATE_MASK = 0x1F;
    private static final int COMMENT_DEPTH_MASK = 0x7F;
    private static final int TEMPLATE_INTERPOLATION_CONTEXT_MASK = 0xFFFFF;

    private static final int COMMENT_DEPTH_SHIFT = 5;
    private static final int TEMPLATE_INTERPOLATION_CONTEXT_SHIFT = 12;

    private static final int TEMPLATE_INTERPOLATION_FRAME_BITS = 5;
    private static final int TEMPLATE_INTERPOLATION_FRAME_MASK = 0x1F;

    // Track previous non-whitespace token for regex/division disambiguation.
    // When we see `/`, if the previous token is an "expression-end" token
    // (identifier, literal, closing delimiter), it's division.
    // Otherwise, it's the start of a regex literal.
    private IElementType lastSignificantToken = null;
    private int commentDepth = 0;
    private int templateInterpolationContext = 0;

    private boolean isSignificant(IElementType type) {
        return type != TokenType.WHITE_SPACE &&
               type != ReScriptTypes.LINE_COMMENT &&
               type != ReScriptTypes.BLOCK_COMMENT;
    }

    private IElementType track(IElementType type) {
        if (isSignificant(type)) lastSignificantToken = type;
        return type;
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
     *
     * Regex examples (previous token is NOT expression-end):
     *   let re = /p/   — EQ `/` → regex
     *   foo(/p/)       — LPAREN `/` → regex
     *   [/a/, /b/]     — LBRACKET or COMMA `/` → regex
     *   x => /p/       — FAT_ARROW `/` → regex
     *   a && /p/       — AMPAMP `/` → regex
     *   start of file  — null → regex
     */
    private boolean isStartRegexSlash() {
        return lastSignificantToken != ReScriptTypes.LIDENT       &&   // x / y
               lastSignificantToken != ReScriptTypes.UIDENT       &&   // Foo / bar
               lastSignificantToken != ReScriptTypes.INT          &&   // 10 / 2
               lastSignificantToken != ReScriptTypes.FLOAT        &&   // 3.0 / 2.0
               lastSignificantToken != ReScriptTypes.RPAREN       &&   // foo() / bar
               lastSignificantToken != ReScriptTypes.RBRACKET;         // arr[0] / 2
    }

    private int currentTemplateInterpolationDepth() {
        return templateInterpolationContext & TEMPLATE_INTERPOLATION_FRAME_MASK;
    }

    private boolean isInTemplateInterpolation() {
        return currentTemplateInterpolationDepth() > 0;
    }

    private void startTemplateInterpolation() {
        if (isInTemplateInterpolation()) {
            templateInterpolationContext =
                ((templateInterpolationContext << TEMPLATE_INTERPOLATION_FRAME_BITS)
                    & TEMPLATE_INTERPOLATION_CONTEXT_MASK) | 1;
        } else {
            templateInterpolationContext = 1;
        }
    }

    private void incrementTemplateInterpolationDepth() {
        int depth = currentTemplateInterpolationDepth();
        if (depth > 0 && depth < TEMPLATE_INTERPOLATION_FRAME_MASK) {
            templateInterpolationContext++;
        }
    }

    private boolean closeTemplateInterpolationBrace() {
        if (!isInTemplateInterpolation()) return false;

        templateInterpolationContext--;
        if (currentTemplateInterpolationDepth() == 0) {
            templateInterpolationContext >>>= TEMPLATE_INTERPOLATION_FRAME_BITS;
            return true;
        }
        return false;
    }

    private int packRestartState(int lexicalState, int commentDepth, int interpolationContext) {
        int packedLexicalState = lexicalState & LEXICAL_STATE_MASK;
        int packedCommentDepth = (commentDepth & COMMENT_DEPTH_MASK) << COMMENT_DEPTH_SHIFT;
        int packedInterpolationContext =
            (interpolationContext & TEMPLATE_INTERPOLATION_CONTEXT_MASK)
                << TEMPLATE_INTERPOLATION_CONTEXT_SHIFT;
        return packedLexicalState | packedCommentDepth | packedInterpolationContext;
    }

    private int unpackLexicalState(int packedState) {
        return packedState & LEXICAL_STATE_MASK;
    }

    private int unpackCommentDepth(int packedState) {
        return (packedState >>> COMMENT_DEPTH_SHIFT) & COMMENT_DEPTH_MASK;
    }

    private int unpackInterpolationContext(int packedState) {
        return (packedState >>> TEMPLATE_INTERPOLATION_CONTEXT_SHIFT)
            & TEMPLATE_INTERPOLATION_CONTEXT_MASK;
    }

    public int getPackedRestartState() {
        return packRestartState(zzLexicalState, commentDepth, templateInterpolationContext);
    }

    public void resetWithPackedRestartState(CharSequence buffer, int start, int end, int packedState) {
        lastSignificantToken = null;
        commentDepth = unpackCommentDepth(packedState);
        templateInterpolationContext = unpackInterpolationContext(packedState);
        reset(buffer, start, end, unpackLexicalState(packedState));
    }

%}

%state REGEX
%state IN_STRING
%state IN_TEMPLATE
%state IN_BLOCK_COMMENT

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

<YYINITIAL> {
    {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
    {LINE_COMMENT}      { return track(ReScriptTypes.LINE_COMMENT); }
    "/*"                { commentDepth = 1; yybegin(IN_BLOCK_COMMENT); }

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
    \"                  { yybegin(IN_STRING); return track(ReScriptTypes.STRING_START); }
    `                   { yybegin(IN_TEMPLATE); return track(ReScriptTypes.TEMPLATE_START); }

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
    "<"                 { return track(ReScriptTypes.LT); }
    ">"                 { return track(ReScriptTypes.GT); }

    // Regex vs division disambiguation: check previous token
    "/"                 { if (isStartRegexSlash()) {
                              yybegin(REGEX);
                              yypushback(1); // un-eat the /
                          } else {
                              return track(ReScriptTypes.SLASH);
                          }
                        }

    "("                 { return track(ReScriptTypes.LPAREN); }
    ")"                 { return track(ReScriptTypes.RPAREN); }
    "{"                 {
                            incrementTemplateInterpolationDepth();
                            return track(ReScriptTypes.LBRACE);
                        }
    "}"                 {
                            if (closeTemplateInterpolationBrace()) {
                                yybegin(IN_TEMPLATE);
                                return track(ReScriptTypes.TEMPLATE_INTERPOLATION_END);
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

// Double-quoted string state
<IN_STRING> {
    \"                  { yybegin(YYINITIAL); return track(ReScriptTypes.STRING_END); }
    \\[\\\"ntbr0]       { return track(ReScriptTypes.STRING_ESCAPE); }
    \\x[0-9a-fA-F]{2}  { return track(ReScriptTypes.STRING_ESCAPE); }
    \\.                 { return track(ReScriptTypes.STRING_ESCAPE); }
    // Consume ordinary string content; stop before `"` closes the string or `\` starts an escape.
    [^\"\\]+            { return ReScriptTypes.STRING_CONTENT; }
}

// Backtick template string state
<IN_TEMPLATE> {
    // A raw backtick closes the current template.
    `                   { yybegin(YYINITIAL); return track(ReScriptTypes.TEMPLATE_END); }

    // `${` starts an interpolation: resume normal ReScript lexing until its matching `}`.
    "${"                {
                            startTemplateInterpolation();
                            yybegin(YYINITIAL);
                            return track(ReScriptTypes.TEMPLATE_INTERPOLATION_START);
                        }

    // Template content is one or more ordinary chars or escaped chars.
    // Ordinary chars exclude `$`, raw backtick, and backslash because those
    // start interpolation, end the template, or start an escaped char.
    ( [^$`\\] | "\\". )+ { return track(ReScriptTypes.TEMPLATE_CONTENT); }

    // A lone `$` is content; only `${` starts interpolation.
    "$"                 { return track(ReScriptTypes.TEMPLATE_CONTENT); }
}

// Nested block comment state: /* /* */ */
<IN_BLOCK_COMMENT> {
    "/*"                { commentDepth++; }
    "*/"                { commentDepth--; if (commentDepth == 0) { yybegin(YYINITIAL); return track(ReScriptTypes.BLOCK_COMMENT); } }
    [^]                 { /* consume */ }
}

[^]                     { return TokenType.BAD_CHARACTER; }
