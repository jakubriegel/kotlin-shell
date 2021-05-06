import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))

    api("org.zeroturnaround:zt-exec:1.11")
    api("org.slf4j:slf4j-api:1.7.26")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC2")
    api("org.jetbrains.kotlinx:kotlinx-io-jvm:0.1.11")

    dokkaJavadocPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.32")

    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.0")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.12.0")
}

sourceSets {
    create("integration") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/integration/kotlin")
            resources.srcDir("src/integration/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

val integration by tasks.creating(Test::class) {
    description = "Runs the integration tests"
    group = JavaBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    mustRunAfter(tasks.test)
    useJUnitPlatform()
}

val dokkaJarConfig: (task: TaskProvider<DokkaTask>) ->  Jar.() -> Unit by rootProject.extra
val dokkaJar by tasks.creating(Jar::class, dokkaJarConfig(tasks.dokkaJavadoc))

val sourcesJarConfig: Jar.() -> Unit by rootProject.extra
val sourcesJar by tasks.creating(Jar::class, sourcesJarConfig)

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
    check { dependsOn(integration) }
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
        "kotlin-shell-core",
        listOf(tasks.jar.get(), sourcesJar, dokkaJar)
    )
)
