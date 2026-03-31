package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.ide.projectView.ProjectViewNestingRulesProvider

class ReScriptFileNestingProvider : ProjectViewNestingRulesProvider {
    override fun addFileNestingRules(consumer: ProjectViewNestingRulesProvider.Consumer) {
        consumer.addNestingRule(".res", ".res.js")
        consumer.addNestingRule(".res", ".res.mjs")
        consumer.addNestingRule(".resi", ".resi.js")
        consumer.addNestingRule(".resi", ".resi.mjs")
    }
}
