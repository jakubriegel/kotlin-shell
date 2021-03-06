import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    `maven-publish`
    id("org.jetbrains.dokka") version "1.4.32"
    id("com.github.johnrengelman.shadow") version "5.1.0" apply false
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
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/jakubriegel/kotlin-shell/$publication")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}

allprojects {
    group = "eu.jrie.jetbrains"
    version = "0.2.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
