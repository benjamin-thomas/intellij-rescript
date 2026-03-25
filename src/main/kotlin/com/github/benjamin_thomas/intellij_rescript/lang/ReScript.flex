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
LOWER_IDENT = [a-z_][a-zA-Z0-9_]*

%%

<YYINITIAL> {
    {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }

    "let"               { return ReScriptTokenTypes.LET; }
    "type"              { return ReScriptTokenTypes.TYPE; }
    "module"            { return ReScriptTokenTypes.MODULE; }
    "switch"            { return ReScriptTokenTypes.SWITCH; }
    "if"                { return ReScriptTokenTypes.IF; }
    "else"              { return ReScriptTokenTypes.ELSE; }

    {LOWER_IDENT}       { return ReScriptTokenTypes.LIDENT; }
}

[^]                     { return TokenType.BAD_CHARACTER; }
