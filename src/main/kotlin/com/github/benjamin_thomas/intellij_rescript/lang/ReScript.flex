package com.github.benjamin_thomas.intellij_rescript.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.github.benjamin_thomas.intellij_rescript.lang.ReScriptTokenTypes;

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
    {LINE_COMMENT}      { return ReScriptTokenTypes.LINE_COMMENT; }
    {BLOCK_COMMENT}     { return ReScriptTokenTypes.BLOCK_COMMENT; }

    "let"               { return ReScriptTokenTypes.LET; }
    "type"              { return ReScriptTokenTypes.TYPE; }
    "module"            { return ReScriptTokenTypes.MODULE; }
    "switch"            { return ReScriptTokenTypes.SWITCH; }
    "if"                { return ReScriptTokenTypes.IF; }
    "else"              { return ReScriptTokenTypes.ELSE; }

    {FLOAT}             { return ReScriptTokenTypes.FLOAT; }
    {INT}               { return ReScriptTokenTypes.INT; }
    {STRING}            { return ReScriptTokenTypes.STRING; }

    {LOWER_IDENT}       { return ReScriptTokenTypes.LIDENT; }
    {UPPER_IDENT}       { return ReScriptTokenTypes.UIDENT; }

    "=="                { return ReScriptTokenTypes.EQEQ; }
    "!="                { return ReScriptTokenTypes.BANGEQ; }
    "<="                { return ReScriptTokenTypes.LTEQ; }
    ">="                { return ReScriptTokenTypes.GTEQ; }
    "->"                { return ReScriptTokenTypes.ARROW; }
    "=>"                { return ReScriptTokenTypes.FAT_ARROW; }
    "|>"                { return ReScriptTokenTypes.PIPE_FORWARD; }
    "+."                { return ReScriptTokenTypes.PLUS_DOT; }
    "-."                { return ReScriptTokenTypes.MINUS_DOT; }
    "*."                { return ReScriptTokenTypes.STAR_DOT; }
    "/."                { return ReScriptTokenTypes.SLASH_DOT; }
    "="                 { return ReScriptTokenTypes.EQ; }
    "+"                 { return ReScriptTokenTypes.PLUS; }
    "-"                 { return ReScriptTokenTypes.MINUS; }
    "*"                 { return ReScriptTokenTypes.STAR; }
    "/"                 { return ReScriptTokenTypes.SLASH; }
    "<"                 { return ReScriptTokenTypes.LT; }
    ">"                 { return ReScriptTokenTypes.GT; }

    "("                 { return ReScriptTokenTypes.LPAREN; }
    ")"                 { return ReScriptTokenTypes.RPAREN; }
    "{"                 { return ReScriptTokenTypes.LBRACE; }
    "}"                 { return ReScriptTokenTypes.RBRACE; }
    "["                 { return ReScriptTokenTypes.LBRACKET; }
    "]"                 { return ReScriptTokenTypes.RBRACKET; }
    ","                 { return ReScriptTokenTypes.COMMA; }
    ";"                 { return ReScriptTokenTypes.SEMICOLON; }
    ":"                 { return ReScriptTokenTypes.COLON; }
    "."                 { return ReScriptTokenTypes.DOT; }
}

[^]                     { return TokenType.BAD_CHARACTER; }
