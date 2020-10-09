package io.gitlab.arturbosch.detekt.rules.style

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

/**
 * This rule ensures class contents are ordered as follows as recommended by the Kotlin
 * [Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#class-layout):
 * - Property declarations and initializer blocks
 * - Secondary constructors
 * - Method declarations
 * - Companion object
 *
 * <noncompliant>
 * class OutOfOrder {
 *     companion object {
 *         const val IMPORTANT_VALUE = 3
 *     }
 *
 *     fun returnX(): Int {
 *         return x
 *     }
 *
 *     private val x = 2
 * }
 * </noncompliant>
 *
 * <compliant>
 * class InOrder {
 *     private val x = 2
 *
 *     fun returnX(): Int {
 *         return x
 *     }
 *
 *     companion object {
 *         const val IMPORTANT_VALUE = 3
 *     }
 * }
 * </compliant>
 */
class ClassOrdering(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        javaClass.simpleName, Severity.Style,
        "Class contents should be in this order: Property declarations/initializer blocks; secondary constructors; " +
                "method declarations then companion objects.",
        Debt.FIVE_MINS
    )

    private val comparator: Comparator<KtDeclaration> = Comparator { dec1: KtDeclaration, dec2: KtDeclaration ->
        if (dec1.priority == null || dec2.priority == null) return@Comparator 0
        compareValues(dec1.priority, dec2.priority)
    }

    override fun visitClassBody(classBody: KtClassBody) {
        super.visitClassBody(classBody)

        comparator.findOutOfOrder(classBody.declarations)?.let {
            report(CodeSmell(issue, Entity.from(classBody), generateMessage(it)))
        }
    }

    private fun generateMessage(misorderedPair: Pair<KtDeclaration, KtDeclaration>): String {
        return "${misorderedPair.first.name} (${misorderedPair.first.printableDeclaration}) should not come before " +
                "${misorderedPair.second.name} (${misorderedPair.second.printableDeclaration})"
    }
}

private fun Comparator<KtDeclaration>.findOutOfOrder(
    declarations: List<KtDeclaration>
): Pair<KtDeclaration, KtDeclaration>? {
    declarations.zipWithNext { a, b -> if (compare(a, b) > 0) return Pair(a, b) }
    return null
}

private val KtDeclaration.printableDeclaration: String
    get() = when (this) {
        is KtProperty -> "property"
        is KtClassInitializer -> "class initializer"
        is KtSecondaryConstructor -> "secondary constructor"
        is KtNamedFunction -> "function"
        is KtObjectDeclaration -> if (isCompanion()) "companion" else ""
        else -> ""
    }

@Suppress("MagicNumber")
private val KtDeclaration.priority: Int?
    get() = when (this) {
        is KtProperty -> 0
        is KtClassInitializer -> 0
        is KtSecondaryConstructor -> 1
        is KtNamedFunction -> 2
        is KtObjectDeclaration -> if (isCompanion()) 3 else null
        else -> null
    }
