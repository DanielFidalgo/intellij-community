// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.k2.codeinsight.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.intentions.AbstractKotlinApplicableIntentionWithContext
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.KotlinApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.applicabilityTarget
import org.jetbrains.kotlin.idea.codeinsight.utils.addTypeArguments
import org.jetbrains.kotlin.idea.codeinsight.utils.getRenderedTypeArguments
import org.jetbrains.kotlin.psi.KtCallExpression

internal class InsertExplicitTypeArgumentsIntention :
    AbstractKotlinApplicableIntentionWithContext<KtCallExpression, String>(KtCallExpression::class) {

    override fun getApplicabilityRange(): KotlinApplicabilityRange<KtCallExpression> = applicabilityTarget { it.calleeExpression }

    override fun isApplicableByPsi(element: KtCallExpression): Boolean = element.typeArguments.isEmpty() && element.calleeExpression != null

    override fun getFamilyName(): String = KotlinBundle.message("add.explicit.type.arguments")

    override fun getActionName(element: KtCallExpression, context: String): String = familyName

    context(KtAnalysisSession)
    override fun prepareContext(element: KtCallExpression): String? = getRenderedTypeArguments(element)

    override fun apply(element: KtCallExpression, context: String, project: Project, editor: Editor?) =
        addTypeArguments(element, context, project)
}
