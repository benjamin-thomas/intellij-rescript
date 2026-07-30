package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptJsxAnnotatorTest : BasePlatformTestCase() {

    override fun getTestDataPath() =
        System.getProperty("user.dir") +
            "/src/test/resources/com/github/benjamin_thomas/intellij_rescript/annotator/fixtures"

    fun testJsxNameAndAttributeColors() {
        myFixture.testHighlighting(false, true, false, "JsxColors.res")
    }
}
