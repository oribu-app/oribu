plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.kotlin.compose)       apply false
    alias(libs.plugins.ksp)                  apply false
    alias(libs.plugins.kotlinter)            apply false
    alias(libs.plugins.aboutlibraries)         apply false
    alias(libs.plugins.aboutlibraries.android) apply false
}

// Applied per-subproject so `lintKotlin`/`formatKotlin` exist to run - the root project has no
// Kotlin sources of its own for the plugin to lint.
subprojects {
    apply(plugin = "org.jmailen.kotlinter")

    tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask>().configureEach {
        exclude { it.file.path.contains("${File.separatorChar}build${File.separatorChar}") }
    }
    tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask>().configureEach {
        exclude { it.file.path.contains("${File.separatorChar}build${File.separatorChar}") }
    }
}
