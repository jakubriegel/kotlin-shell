import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.61"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4" apply false
    id("org.jetbrains.dokka") version "0.9.17"
    id("com.github.johnrengelman.shadow") version "5.1.0" apply false
}

val dokkaConfig: DokkaTask.() -> Unit by extra {
    {
        outputFormat = "javadoc"
        outputDirectory = "${project.buildDir}/javadoc"
        noStdlibLink = true
        jdkVersion = 8
        reportUndocumented = false
        sourceDirs = files("src/main/kotlin")
    }
}

val dokkaJarConfig: (TaskProvider<DokkaTask>) ->  Jar.() -> Unit by extra {
    { task: TaskProvider<DokkaTask> ->
        {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            setProperty("classifier", "javadoc")
            from(task)
        }
    }
}

val sourcesJarConfig: Jar.() -> Unit by extra {
    {
        group = JavaBasePlugin.BUILD_TASK_NAME
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        setProperty("classifier", "sources")
        from(project.sourceSets["main"].allSource)
    }
}

val publicationConfig by extra {
    { project: Project, publication: String, artifactsToUse: List<Jar> ->
        Action<PublishingExtension> {
            publications {
                create<MavenPublication>(publication) {
                    from(project.components["kotlin"])
                    groupId = project.group.toString()
                    artifactId = publication
                    version = project.version.toString()
                    setArtifacts(artifactsToUse)
                }
            }
        }
    }
}

val uploadConfig by extra {
    { publication: String, packageDescription: String ->
        Action<BintrayExtension> {
            user = System.getenv("BINTRAY_USER")
            key = System.getenv("BINTRAY_KEY")
            override = true
            publish = true
            setPublications(publication)
            pkg (delegateClosureOf<BintrayExtension.PackageConfig> {
                repo = "kotlin-shell"
                name = publication
                userOrg = "jakubriegel"
                websiteUrl = ""
                githubRepo = "jakubriegel/kotlin-shell"
                vcsUrl = "https://github.com/jakubriegel/kotlin-shell"
                description = packageDescription
                setLabels("kotlin", "shell", "script", "process-management", "pipeline")
                setLicenses("apache2")
                desc = description
            })

        }
    }
}

allprojects {
    group = "eu.jrie.jetbrains"
    version = "0.2"

    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        mavenCentral()
    }
}

subprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
