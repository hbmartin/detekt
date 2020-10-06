package io.github.detekt.report.sarif

import io.github.detekt.sarif4j.Run
import io.github.detekt.sarif4j.SarifSchema210
import io.github.detekt.sarif4j.Tool
import io.github.detekt.sarif4j.ToolComponent
import io.github.detekt.sarif4j.ToolComponentReference
import io.github.detekt.tooling.api.VersionProvider
import java.net.URI
import java.util.UUID

const val SCHEMA_URL = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json"

fun sarif(init: SarifSchema210.() -> Unit): SarifSchema210 {
    val sarif = SarifSchema210().apply {
        version = SarifSchema210.Version._2_1_0
        `$schema` = URI.create(SCHEMA_URL)
    }
    return sarif.apply(init)
}

typealias SarifIssue = io.github.detekt.sarif4j.Result

fun result(init: SarifIssue.() -> Unit): SarifIssue = SarifIssue().apply(init)

fun detektRun(init: Run.() -> Unit): Run = Run().apply(init)

fun tool(init: Tool.() -> Unit): Tool = Tool().apply(init)

fun component(init: ToolComponent.() -> Unit): ToolComponent = ToolComponent().apply(init)

val DetektToolComponent = component {
    guid = UUID.randomUUID().toString()
    name = "detekt"
    fullName = "detekt"
    organization = "detekt"
    language = "Kotlin"
    version = VersionProvider.load().current()
    downloadUri = URI.create("https://github.com/detekt/detekt/releases/download/v$version/detekt")
    informationUri = URI.create("https://detekt.github.io/detekt")
}

val DetektToolReference = ToolComponentReference().apply {
    guid = DetektToolComponent.guid
    name = DetektToolComponent.name
}

fun detektSarif(init: Run.() -> Unit): SarifSchema210 {
    return sarif {
        runs = mutableListOf(
            detektRun {
                tool = tool {
                    driver = DetektToolComponent
                }
                apply(init)
            }
        )
    }
}
