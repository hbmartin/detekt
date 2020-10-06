package io.github.detekt.report.sarif

import io.github.detekt.sarif4j.MultiformatMessageString
import io.github.detekt.sarif4j.ReportingDescriptor
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.MultiRule
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleSetId
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import java.util.ServiceLoader
import java.util.UUID

fun ruleDescriptors(config: Config): HashMap<String, ReportingDescriptor> {
    val sets = ServiceLoader.load(RuleSetProvider::class.java)
        .map { it.instance(config.subConfig(it.ruleSetId)) }
    val descriptors = HashMap<String, ReportingDescriptor>()
    for (ruleSet in sets) {
        for (rule in ruleSet.rules) {
            if (rule is MultiRule) {
                descriptors.putAll(rule.toDescriptors(ruleSet.id).associateBy { it.name })
            } else {
                assert(rule is Rule)
                val descriptor = (rule as Rule).toDescriptor(ruleSet.id)
                descriptors[descriptor.name] = descriptor
            }
        }
    }
    return descriptors
}

fun descriptor(init: ReportingDescriptor.() -> Unit) = ReportingDescriptor().apply(init)

fun MultiRule.toDescriptors(ruleSetId: RuleSetId): List<ReportingDescriptor> =
    this.rules.map { it.toDescriptor(ruleSetId) }

fun Rule.toDescriptor(ruleSetId: RuleSetId): ReportingDescriptor = descriptor {
    guid = UUID.randomUUID().toString()
    name = "$ruleSetId>$ruleId"
    shortDescription = MultiformatMessageString().apply {
        text = issue.description.substringBefore(System.lineSeparator())
    }
    fullDescription = MultiformatMessageString().apply { markdown = issue.description }
}
