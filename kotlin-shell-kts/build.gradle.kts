import com.jfrog.bintray.gradle.BintrayExtension

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.jfrog.bintray")
    id("org.jetbrains.dokka")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))

    compileOnly(kotlin("compiler"))
    compileOnly(kotlin("scripting-jvm-host"))
    compileOnly(kotlin("script-util"))

    api(project(":kotlin-shell-core"))
    api("org.slf4j:slf4j-nop:1.7.26")
}

tasks.shadowJar {
    setProperty("archiveName", "ksh.jar")
    dependencies {
        exclude(dependency("org.jetbrains.kotlin::"))
    }
}

val bintrayPublication = "ksh"

publishing {
    publications {
        create<MavenPublication>(bintrayPublication) {
            from(components["kotlin"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            setArtifacts(listOf(tasks.shadowJar.get()))
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")
    setPublications(bintrayPublication)
    publish = true
    pkg (delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "kotlin-shell"
        name = "kotlin-shell-kts"
        userOrg = "jakubriegel"
        websiteUrl = ""
        githubRepo = "jakubriegel/kotlin-shell"
        vcsUrl = "https://github.com/jakubriegel/kotlin-shell"
        description = "Script definition for Kotlin shell scripting"
        setLabels("kotlin", "shell", "pipeline", "process-management", "script")
        setLicenses("apache2")
        desc = description
    })
}
