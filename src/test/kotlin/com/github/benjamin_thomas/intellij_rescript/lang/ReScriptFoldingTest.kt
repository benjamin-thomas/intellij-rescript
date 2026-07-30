package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptFoldingTest : BasePlatformTestCase() {

    override fun getTestDataPath() =
        System.getProperty("user.dir") + "/src/test/resources/com/github/benjamin_thomas/intellij_rescript/folding/fixtures"

    fun testLetWithBlock() = myFixture.testFolding("$testDataPath/LetWithBlock.res")

    fun testBlockComment() = myFixture.testFolding("$testDataPath/BlockComment.res")

    fun testJsxElementWithChildren() = myFixture.testFolding("$testDataPath/JsxElementWithChildren.res")

    fun testJsxFragment() = myFixture.testFolding("$testDataPath/JsxFragment.res")

    fun testJsxSelfClosingMultiline() = myFixture.testFolding("$testDataPath/JsxSelfClosingMultiline.res")

    fun testJsxSingleLine() = myFixture.testFolding("$testDataPath/JsxSingleLine.res")

    fun testJsxNestedElements() = myFixture.testFolding("$testDataPath/JsxNestedElements.res")

    fun testJsxPlaceholders() = myFixture.testFolding("$testDataPath/JsxPlaceholders.res")

    fun testJsxBraceInteraction() = myFixture.testFolding("$testDataPath/JsxBraceInteraction.res")
}
