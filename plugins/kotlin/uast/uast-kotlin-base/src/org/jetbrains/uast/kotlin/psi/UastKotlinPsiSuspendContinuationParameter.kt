// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.uast.kotlin.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.kotlin.BaseKotlinUastResolveProviderService
import org.jetbrains.uast.kotlin.lz

@ApiStatus.Internal
class UastKotlinPsiSuspendContinuationParameter private constructor(
    parameterType: PsiType,
    parent: PsiElement,
    suspendFunction: KtFunction,
) : UastKotlinPsiParameterBase<KtFunction>(
    name = SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME,
    type = parameterType,
    parent = parent,
    ktOrigin = suspendFunction,
    language = suspendFunction.language,
    isVarArgs = false,
    ktDefaultValue = null
) {
    private val _annotations: Array<PsiAnnotation> by lz {
        arrayOf(
            UastFakeLightNullabilityAnnotation(KtTypeNullability.NON_NULLABLE, this)
        )
    }

    override fun getAnnotations(): Array<PsiAnnotation> {
        return _annotations
    }

    override fun hasAnnotation(fqn: String): Boolean {
        return _annotations.find { it.hasQualifiedName(fqn) } != null
    }

    companion object {
        fun create(
            parent: PsiModifierListOwner,
            suspendFunction: KtFunction,
        ): PsiParameter {
            val service = ApplicationManager.getApplication().getService(BaseKotlinUastResolveProviderService::class.java)
            val parameterType = service.getSuspendContinuationType(suspendFunction, parent) ?: PsiTypes.nullType()
            return UastKotlinPsiSuspendContinuationParameter(parameterType, parent, suspendFunction)
        }
    }
}
