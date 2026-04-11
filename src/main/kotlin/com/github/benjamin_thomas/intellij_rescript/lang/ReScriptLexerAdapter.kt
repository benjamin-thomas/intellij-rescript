package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lexer.LexerBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class ReScriptLexerAdapter : LexerBase() {
    private val flex = _ReScriptLexer(null)

    private var text: CharSequence = ""
    private var tokenStart = 0
    private var tokenEnd = 0
    private var bufferEnd = 0
    private var tokenType: IElementType? = null
    private var state = 0
    private var failed = false

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        text = buffer
        tokenStart = startOffset
        tokenEnd = startOffset
        bufferEnd = endOffset
        failed = false
        tokenType = null
        flex.resetWithPackedRestartState(buffer, startOffset, endOffset, initialState)
    }

    override fun getState(): Int {
        locateToken()
        return state
    }

    override fun getTokenType(): IElementType? {
        locateToken()
        return tokenType
    }

    override fun getTokenStart(): Int {
        locateToken()
        return tokenStart
    }

    override fun getTokenEnd(): Int {
        locateToken()
        return tokenEnd
    }

    override fun advance() {
        locateToken()
        tokenType = null
    }

    override fun getBufferSequence(): CharSequence = text

    override fun getBufferEnd(): Int = bufferEnd

    private fun locateToken() {
        if (tokenType != null) return

        tokenStart = tokenEnd
        if (failed) return

        try {
            state = flex.packedRestartState
            tokenType = flex.advance()
            tokenEnd = flex.tokenEnd
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (t: Throwable) {
            failed = true
            tokenType = TokenType.BAD_CHARACTER
            tokenEnd = bufferEnd
            LOG.warn(flex.javaClass.name, t)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ReScriptLexerAdapter::class.java)
    }
}
