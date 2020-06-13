package com.huawei.rri.fixbot.ruleset.huawei.rules

import com.huawei.rri.fixbot.ruleset.huawei.constants.Warnings
import com.huawei.rri.fixbot.ruleset.huawei.constants.Warnings.MISSING_KDOC_CLASS_ELEMENTS
import com.huawei.rri.fixbot.ruleset.huawei.constants.Warnings.MISSING_KDOC_TOP_LEVEL
import com.huawei.rri.fixbot.ruleset.huawei.utils.getAllChildrenWithType
import com.huawei.rri.fixbot.ruleset.huawei.utils.getFirstChildWithType
import com.huawei.rri.fixbot.ruleset.huawei.utils.getIdentifierName
import com.huawei.rri.fixbot.ruleset.huawei.utils.isAccessibleOutside
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import config.rules.RulesConfig
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * This rule checks the following features in KDocs:
 * 1) All top-level (file level) functions and classes with public or internal access should have KDoc
 * 2) All internal elements in class like class, property or function should be documented with KDoc
 */
class KdocComments : Rule("kdoc-comments") {

    private lateinit var confiRules: List<RulesConfig>
    private lateinit var emitWarn: ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit)
    private var isFixMode: Boolean = false

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        params: KtLint.Params,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        confiRules = params.rulesConfigList!!
        emitWarn = emit
        isFixMode = autoCorrect

        when (node.elementType) {
            FILE -> checkTopLevelDoc(node)
            CLASS -> checkClassElements(node)
        }
    }

    private fun checkClassElements(node: ASTNode) {
        val modifier = node.getFirstChildWithType(MODIFIER_LIST)
        val classBody = node.getFirstChildWithType(CLASS_BODY)

        // if parent class is public or internal than we can check it's internal code elements
        if (classBody != null && isAccessibleOutside(modifier)) {
            (classBody.getAllChildrenWithType(CLASS) + classBody.getAllChildrenWithType(FUN) + classBody.getAllChildrenWithType(PROPERTY))
                .forEach { checkDoc(it, MISSING_KDOC_CLASS_ELEMENTS) }
        }
    }

    private fun checkTopLevelDoc(node: ASTNode) =
        // checking that all top level class declarations and functions have kDoc
        (node.getAllChildrenWithType(CLASS) + node.getAllChildrenWithType(FUN))
            .forEach { checkDoc(it, MISSING_KDOC_TOP_LEVEL) }


    /**
     * raises warning if protected, public or internal code element does not have a Kdoc
     */
    private fun checkDoc(node: ASTNode, warning: Warnings) {
        val kDoc = node.getFirstChildWithType(KDOC)
        val modifier = node.getFirstChildWithType(MODIFIER_LIST)
        val name = node.getIdentifierName()

        if (isAccessibleOutside(modifier) && kDoc == null) {
            warning.warn(confiRules, emitWarn, isFixMode, name!!.text, node.startOffset)
        }
    }
}
