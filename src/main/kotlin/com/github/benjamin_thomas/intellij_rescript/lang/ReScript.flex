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

WHITE_SPACE = [ \t\n\r]+
LINE_COMMENT = "//" [^\n]*
BLOCK_COMMENT = "/*" [^*]* ("*" [^/] [^*]*)* "*/"
LOWER_IDENT = [a-z_][a-zA-Z0-9_]*
UPPER_IDENT = [A-Z][a-zA-Z0-9_]*
INT = [0-9]+
FLOAT = [0-9]+ "." [0-9]+
STRING = \" ([^\"\\\n] | \\.)* \"

%%

<YYINITIAL> {
    {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
    {LINE_COMMENT}      { return ReScriptTypes.LINE_COMMENT; }
    {BLOCK_COMMENT}     { return ReScriptTypes.BLOCK_COMMENT; }

    "let"               { return ReScriptTypes.LET; }
    "type"              { return ReScriptTypes.TYPE; }
    "module"            { return ReScriptTypes.MODULE; }
    "switch"            { return ReScriptTypes.SWITCH; }
    "if"                { return ReScriptTypes.IF; }
    "else"              { return ReScriptTypes.ELSE; }

    {FLOAT}             { return ReScriptTypes.FLOAT; }
    {INT}               { return ReScriptTypes.INT; }
    {STRING}            { return ReScriptTypes.STRING; }

    "_"                 { return ReScriptTypes.UNDERSCORE; }
    {LOWER_IDENT}       { return ReScriptTypes.LIDENT; }
    {UPPER_IDENT}       { return ReScriptTypes.UIDENT; }

    "=="                { return ReScriptTypes.EQEQ; }
    "!="                { return ReScriptTypes.BANGEQ; }
    "<="                { return ReScriptTypes.LTEQ; }
    ">="                { return ReScriptTypes.GTEQ; }
    "->"                { return ReScriptTypes.ARROW; }
    "=>"                { return ReScriptTypes.FAT_ARROW; }
    "|>"                { return ReScriptTypes.PIPE_FORWARD; }
    "+."                { return ReScriptTypes.PLUS_DOT; }
    "-."                { return ReScriptTypes.MINUS_DOT; }
    "*."                { return ReScriptTypes.STAR_DOT; }
    "/."                { return ReScriptTypes.SLASH_DOT; }
    "="                 { return ReScriptTypes.EQ; }
    "+"                 { return ReScriptTypes.PLUS; }
    "-"                 { return ReScriptTypes.MINUS; }
    "*"                 { return ReScriptTypes.STAR; }
    "/"                 { return ReScriptTypes.SLASH; }
    "<"                 { return ReScriptTypes.LT; }
    ">"                 { return ReScriptTypes.GT; }

    "("                 { return ReScriptTypes.LPAREN; }
    ")"                 { return ReScriptTypes.RPAREN; }
    "{"                 { return ReScriptTypes.LBRACE; }
    "}"                 { return ReScriptTypes.RBRACE; }
    "["                 { return ReScriptTypes.LBRACKET; }
    "]"                 { return ReScriptTypes.RBRACKET; }
    ","                 { return ReScriptTypes.COMMA; }
    ";"                 { return ReScriptTypes.SEMICOLON; }
    ":"                 { return ReScriptTypes.COLON; }
    "..."               { return ReScriptTypes.DOTDOTDOT; }
    "."                 { return ReScriptTypes.DOT; }
    "@"                 { return ReScriptTypes.AT; }
    "~"                 { return ReScriptTypes.TILDE; }
    "|"                 { return ReScriptTypes.PIPE; }
    "!"                 { return ReScriptTypes.BANG; }
    "?"                 { return ReScriptTypes.QUESTION; }
    "#"                 { return ReScriptTypes.HASH; }
    "'"                 { return ReScriptTypes.TICK; }
    "%%"                { return ReScriptTypes.PCT_PCT; }
}

[^]                     { return TokenType.BAD_CHARACTER; }
