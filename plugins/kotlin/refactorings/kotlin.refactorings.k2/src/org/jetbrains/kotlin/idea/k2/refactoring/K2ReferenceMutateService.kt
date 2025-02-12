// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.k2.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.IncorrectOperationException
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.KtSymbolBasedReference
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.idea.base.analysis.api.utils.invokeShortening
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinNameSuggester
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinOptimizeImportsFacility
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.idea.refactoring.intentions.OperatorToFunctionConverter
import org.jetbrains.kotlin.idea.refactoring.rename.KtReferenceMutateServiceBase
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

/**
 * At the moment, this implementation of [org.jetbrains.kotlin.idea.references.KtReferenceMutateService] is not able to do some of the
 * required operations. It is OK and on purpose - this functionality will be added later.
 */
@Suppress("UNCHECKED_CAST")
internal class K2ReferenceMutateService : KtReferenceMutateServiceBase() {
    override fun bindToElement(ktReference: KtReference, element: PsiElement): PsiElement = when (ktReference) {
        is KtSimpleNameReference -> bindToElement(ktReference, element, KtSimpleNameReference.ShorteningMode.DELAYED_SHORTENING)
        is KDocReference -> bindToElement(ktReference, element)
        else -> throw IncorrectOperationException()
    }

    @OptIn(KtAllowAnalysisFromWriteAction::class)
    private fun <R : KtElement> KtFile.withOptimizedImports(replacement: () -> R?): PsiElement? = allowAnalysisFromWriteAction {
        fun KtFile.unusedImports(): Set<KtImportDirective> =
            KotlinOptimizeImportsFacility.getInstance().analyzeImports(this)?.unusedImports?.toSet().orEmpty()

        val unusedImportsBefore = unusedImports()
        val newElement = replacement() ?: return null
        val unusedImportsAfter = unusedImports()
        val importsToRemove =  unusedImportsAfter - unusedImportsBefore
        importsToRemove.forEach(PsiElement::delete)
        val newShortenings = analyze(newElement) { collectPossibleReferenceShorteningsInElement(newElement) }
        return newShortenings.invokeShortening().firstOrNull() ?: newElement
    }

    @RequiresWriteLock
    private fun bindToElement(docReference: KDocReference, targetElement: PsiElement): PsiElement {
        val docElement = docReference.element
        val targetFqn = targetElement.kotlinFqName ?: return docElement
        if (targetFqn.isRoot) return docElement
        return docElement.containingKtFile.withOptimizedImports {
            val newDocReference = KDocElementFactory(targetElement.project).createNameFromText(targetFqn.asString())
            docReference.expression.replaced(newDocReference)
        } ?: docElement
    }

    @RequiresWriteLock
    override fun bindToFqName(
        simpleNameReference: KtSimpleNameReference,
        fqName: FqName,
        shorteningMode: KtSimpleNameReference.ShorteningMode, // not supported in K2, it always does FORCED_SHORTENING
        targetElement: PsiElement?
    ): PsiElement {
        if (targetElement !is KtElement) operationNotSupportedInK2Error() // TODO fix reference shortener for non-Kotlin target elements
        val expression = simpleNameReference.expression
        if (fqName.isRoot) return expression
        val importDirective = expression.parentOfType<KtImportDirective>(withSelf = false)
        if (importDirective != null) return importDirective.replaceWith(fqName) ?: expression
        return expression.containingKtFile.withOptimizedImports {
            val anchorElement = expression.parentOfType<KtUserType>(withSelf = false)
                                ?: expression.qualifiedReferenceExpression()
                                ?: expression.parent as? KtCallExpression?
                                ?: expression
            when (anchorElement) {
                is KtUserType -> anchorElement.replaceWith(fqName)
                is KtDotQualifiedExpression -> anchorElement.replaceWith(fqName)
                is KtCallExpression -> anchorElement.replaceWith(fqName)
                is KtSimpleNameExpression -> anchorElement.replaceWith(fqName)
                else -> null
            }
        } ?: expression
    }

    private fun KtSimpleNameExpression.qualifiedReferenceExpression(): KtExpression? {
        val qualifiedExpr = parentOfType<KtDotQualifiedExpression>(withSelf = false) ?: return null
        return if (qualifiedExpr.receiverExpression is KtCallExpression && qualifiedExpr.selectorExpression is KtCallExpression) {
            qualifiedExpr.receiverExpression // <caret>X().y() here we want to bind X only
        } else qualifiedExpr
    }

    private fun KtImportDirective.replaceWith(fqName: FqName): KtExpression? {
        val newImportReferenceExpression = KtPsiFactory(project).createExpression(fqName.asString())
        return importedReference?.replaced(newImportReferenceExpression)
    }

    private fun KtTypeElement.replaceWith(fqName: FqName): KtTypeElement {
        val newReference = KtPsiFactory(project).createType(fqName.asString()).typeElement
                           ?: error("Could not create type from $fqName")
        return replaced(newReference)
    }

    private fun KtDotQualifiedExpression.replaceWith(fqName: FqName): KtExpression? {
        val selectorExpression = selectorExpression ?: return null
        val newExpression = when (selectorExpression) {
            is KtSimpleNameExpression -> selectorExpression.replaceShortName(fqName)
            is KtCallExpression -> selectorExpression.replaceShortName(fqName)
            else -> null
        } ?: return null
        return replaceWithQualified(fqName, newExpression)
    }

    private fun KtExpression.replaceWithQualified(fqName: FqName, selectorExpression: KtExpression): KtExpression {
        val parentFqName = fqName.parent()
        if (parentFqName.isRoot) return selectorExpression
        val packageName = fqName.parent().asString()
        val newQualifiedExpression = KtPsiFactory(project).createExpression("$packageName.${selectorExpression.text}")
        return replaced(newQualifiedExpression)
    }

    private fun KtCallExpression.replaceWith(fqName: FqName): KtExpression {
        val newCall = replaceShortName(fqName)
        return newCall.replaceWithQualified(fqName, newCall)
    }

    private fun KtCallExpression.replaceShortName(fqName: FqName): KtExpression {
        val psiFactory = KtPsiFactory(project)
        val newName = psiFactory.createSimpleName(fqName.shortName().asString())
        calleeExpression?.replace(newName)
        return this
    }

    private fun KtSimpleNameExpression.replaceWith(fqName: FqName): KtExpression {
        val newNameExpr = replaceShortName(fqName)
        return newNameExpr.replaceWithQualified(fqName, newNameExpr)
    }

    private fun KtSimpleNameExpression.replaceShortName(fqName: FqName): KtExpression {
        val newNameExpression = KtPsiFactory(project).createSimpleName(fqName.shortName().asString())
        return replaced(newNameExpression)
    }

    override fun KtSimpleReference<KtNameReferenceExpression>.suggestVariableName(
        expr: KtExpression,
        context: PsiElement): String {
        @OptIn(KtAllowAnalysisOnEdt::class)
        allowAnalysisOnEdt {
            analyze(expr) {
                return KotlinNameSuggester(KotlinNameSuggester.Case.CAMEL).suggestExpressionNames(expr).first()
            }
        }
    }

    override fun handleElementRename(ktReference: KtReference, newElementName: String): PsiElement? {
        @OptIn(KtAllowAnalysisFromWriteAction::class)
        allowAnalysisFromWriteAction {
            if (ktReference is KtSymbolBasedReference) {
                @OptIn(KtAllowAnalysisOnEdt::class)
                allowAnalysisOnEdt {
                    analyze(ktReference.element) {
                        val symbol = ktReference.resolveToSymbol()
                        if (symbol is KtSyntheticJavaPropertySymbol || symbol is KtJavaFieldSymbol) {
                            val newName = (ktReference as? KtSimpleReference<KtNameReferenceExpression>)?.getAdjustedNewName(newElementName)
                            if (newName == null) {
                                return (ktReference as? KtSimpleReference<KtNameReferenceExpression>)?.renameToOrdinaryMethod(newElementName)
                            } else {
                                return super.handleElementRename(ktReference, newName.asString())
                            }
                        }
                    }
                }
            }

            return super.handleElementRename(ktReference, newElementName)
        }
    }

    override fun replaceWithImplicitInvokeInvocation(newExpression: KtDotQualifiedExpression): KtExpression? =
      OperatorToFunctionConverter.replaceExplicitInvokeCallWithImplicit(newExpression)

    private fun operationNotSupportedInK2Error(): Nothing {
        throw IncorrectOperationException("K2 plugin does not yet support this operation")
    }
}