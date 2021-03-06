import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.jetbrains.dokka")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))
    api(kotlin("main-kts"))

    api(project(":kotlin-shell-core"))
    api("org.slf4j:slf4j-nop:1.7.26")
}

val dokkaJarConfig: (task: TaskProvider<DokkaTask>) ->  Jar.() -> Unit by rootProject.extra
val dokkaJar by tasks.creating(Jar::class, dokkaJarConfig(tasks.dokkaJavadoc))

val sourcesJarConfig: Jar.() -> Unit by rootProject.extra
val sourcesJar by tasks.creating(Jar::class, sourcesJarConfig)

tasks {
    val jarBaseName = "kotlin-shell-kts"
    val relocatedPackagesRoot = "$group.relocated"
    val packagesToRelocate = listOf(
        "org.jetbrains.kotlin",
        "kotlin.script.dependencies",
//        "kotlin.script.experimental.annotations",
//        "kotlin.script.experimental.api",
//        "kotlin.script.experimental.host",
//        "kotlin.script.experimental.jvm",
//        "kotlin.script.experimental.util",
        "kotlin.script.experimental.dependencies",
        "kotlin.script.experimental.jvmhost",
        "kotlin.script.experimental.location",
        "fr.jayasoft.ivy",
        "org.intellij",
        "org.jetbrains.annotations",
        "org.zeroturnaround",
        "org.slf4j"
//    commented ones cause errors on script compilation
    )

    fun ShadowJar.configureShadow(classifier: String) {
        setProperty("archiveBaseName", jarBaseName)
        setProperty("archiveClassifier", classifier)
    }

    shadowJar {
        configureShadow("all")

        dependencies {
            // stdlib
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7:"))

            // reflect
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:"))

            // main-kts
            exclude("META-INF/kotlin/script/templates/org.jetbrains.kotlin.mainKts.MainKtsScript.classname")
            exclude(dependency("org.jetbrains.kotlin:kotlin-scripting-jvm-host-embeddable:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-compiler-embeddable:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-script-runtime:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-daemon-embeddable:"))
            exclude(dependency("org.jetbrains.intellij.deps:trove4j:1.0.20181211"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-scripting-jvm:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-scripting-common:"))

            // shell-core
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib:"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-common:"))
            exclude(dependency("org.jetbrains:annotations:"))
        }

        packagesToRelocate.forEach {
            relocate(it, "$relocatedPackagesRoot.$it")
        }
    }
}

artifacts {
    archives(sourcesJar)
    archives(dokkaJar)
    archives(tasks.jar)
}

val publicationConfig: (Project, String, List<Jar>) -> Action<PublishingExtension> by rootProject.extra
publishing(
    publicationConfig(
        project,
        "kotlin-shell-kts",
        listOf(tasks.jar.get(), sourcesJar, dokkaJar, tasks.shadowJar.get())
    )
)
