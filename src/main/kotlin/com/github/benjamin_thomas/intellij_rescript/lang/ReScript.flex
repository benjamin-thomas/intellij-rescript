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
    // Track previous non-whitespace token for regex/division disambiguation.
    // When we see `/`, if the previous token is an "expression-end" token
    // (identifier, literal, closing delimiter), it's division.
    // Otherwise, it's the start of a regex literal.
    private IElementType lastSignificantToken = null;

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
%}

%state REGEX
%state IN_STRING
%state IN_TEMPLATE

WHITE_SPACE = [ \t\n\r]+
LINE_COMMENT = "//" [^\n]*
BLOCK_COMMENT = "/*" [^*]* ("*" [^/] [^*]*)* "*/"
LOWER_IDENT = [a-z_][a-zA-Z0-9_]*
UPPER_IDENT = [A-Z][a-zA-Z0-9_]*
INT = [0-9]+
FLOAT = [0-9]+ "." [0-9]+

%%

<YYINITIAL> {
    {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
    {LINE_COMMENT}      { return track(ReScriptTypes.LINE_COMMENT); }
    {BLOCK_COMMENT}     { return track(ReScriptTypes.BLOCK_COMMENT); }

    "let"               { return track(ReScriptTypes.LET); }
    "type"              { return track(ReScriptTypes.TYPE); }
    "module"            { return track(ReScriptTypes.MODULE); }
    "open"              { return track(ReScriptTypes.OPEN); }
    "include"           { return track(ReScriptTypes.INCLUDE); }
    "external"          { return track(ReScriptTypes.EXTERNAL); }
    "exception"         { return track(ReScriptTypes.EXCEPTION); }
    "rec"               { return track(ReScriptTypes.REC); }
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
    {INT}               { return track(ReScriptTypes.INT); }
    \"                  { yybegin(IN_STRING); return track(ReScriptTypes.STRING_START); }
    `                   { yybegin(IN_TEMPLATE); return track(ReScriptTypes.TEMPLATE_START); }

    "_"                 { return track(ReScriptTypes.UNDERSCORE); }
    {LOWER_IDENT}       { return track(ReScriptTypes.LIDENT); }
    {UPPER_IDENT}       { return track(ReScriptTypes.UIDENT); }

    "&&"                { return track(ReScriptTypes.AMPAMP); }
    "||"                { return track(ReScriptTypes.PIPEPIPE); }
    "=="                { return track(ReScriptTypes.EQEQ); }
    "!="                { return track(ReScriptTypes.BANGEQ); }
    "<="                { return track(ReScriptTypes.LTEQ); }
    ">="                { return track(ReScriptTypes.GTEQ); }
    "->"                { return track(ReScriptTypes.ARROW); }
    "=>"                { return track(ReScriptTypes.FAT_ARROW); }
    "|>"                { return track(ReScriptTypes.PIPE_FORWARD); }
    "+."                { return track(ReScriptTypes.PLUS_DOT); }
    "-."                { return track(ReScriptTypes.MINUS_DOT); }
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
    "{"                 { return track(ReScriptTypes.LBRACE); }
    "}"                 { return track(ReScriptTypes.RBRACE); }
    "["                 { return track(ReScriptTypes.LBRACKET); }
    "]"                 { return track(ReScriptTypes.RBRACKET); }
    ","                 { return track(ReScriptTypes.COMMA); }
    ";"                 { return track(ReScriptTypes.SEMICOLON); }
    ":"                 { return track(ReScriptTypes.COLON); }
    "..."               { return track(ReScriptTypes.DOTDOTDOT); }
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
    \n                  { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
    [^\"\\\n]+          { return track(ReScriptTypes.STRING_CONTENT); }
}

// Backtick template string state
<IN_TEMPLATE> {
    `                   { yybegin(YYINITIAL); return track(ReScriptTypes.TEMPLATE_END); }
    [^`]+               { return track(ReScriptTypes.TEMPLATE_CONTENT); }
}

[^]                     { return TokenType.BAD_CHARACTER; }
