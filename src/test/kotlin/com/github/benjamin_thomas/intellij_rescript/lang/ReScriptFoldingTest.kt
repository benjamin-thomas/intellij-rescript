package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptFoldingTest : BasePlatformTestCase() {

    override fun getTestDataPath() =
        System.getProperty("user.dir") + "/src/test/resources/com/github/benjamin_thomas/intellij_rescript/folding/fixtures"

    fun testLetWithBlock() = myFixture.testFolding("$testDataPath/LetWithBlock.res")

    fun testBlockComment() = myFixture.testFolding("$testDataPath/BlockComment.res")
}
