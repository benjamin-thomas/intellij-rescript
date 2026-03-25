package com.github.benjamin_thomas.intellij_rescript

import kotlin.test.Test
import kotlin.test.assertEquals

class ReScriptFileTypeTest {
    private val fileType = ReScriptFileType.INSTANCE

    @Test
    fun `it should have the correct name`() {
        assertEquals("ReScript", fileType.name)
    }

    @Test
    fun `it should have the correct description`() {
        assertEquals("ReScript language file", fileType.description)
    }

    @Test
    fun `it should have the correct default extension`() {
        assertEquals("res", fileType.defaultExtension)
    }
}
