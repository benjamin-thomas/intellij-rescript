package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptGotoRelatedTest : BasePlatformTestCase() {

    private fun getRelatedItems() =
        ReScriptGotoRelatedProvider().getItems(myFixture.file)

    fun testResGoesToResi() {
        myFixture.configureByText("Foo.resi", "let x: int")
        myFixture.configureByText("Foo.res", "let x = 1<caret>")
        val items = getRelatedItems()
        assertEquals(1, items.size)
        assertEquals("Foo.resi", items[0].element?.containingFile?.name)
    }

    fun testResiGoesToRes() {
        myFixture.configureByText("Foo.res", "let x = 1")
        myFixture.configureByText("Foo.resi", "let x: int<caret>")
        val items = getRelatedItems()
        assertEquals(1, items.size)
        assertEquals("Foo.res", items[0].element?.containingFile?.name)
    }

    fun testNoRelatedWhenCounterpartMissing() {
        myFixture.configureByText("Foo.res", "let x = 1<caret>")
        val items = getRelatedItems()
        assertEquals(0, items.size)
    }
}
