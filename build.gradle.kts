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

                    pom {
                        name.set("kotlin-shell")
                        description.set("Tool for performing shell-like programing in Kotlin")
                        url.set("https://github.com/jakubriegel/kotlin-shell")
                        licenses {
                            license {
                                name.set("Apache License")
                                url.set("https://raw.githubusercontent.com/jakubriegel/kotlin-shell/master/LICENSE")
                            }
                        }
                        scm {
                            url.set("https://github.com/jakubriegel/kotlin-shell")
                        }
                        developers {
                            developer {
                                name.set("Jakub Riegel")
                                url.set("https://github.com/jakubriegel")
                            }
                        }
                    }
                    setArtifacts(artifactsToUse)
                }
            }
            repositories {
                maven {
                    name = "ossStaging"
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = System.getenv("SONATYPE_NEXUS_USERNAME")
                        password = System.getenv("SONATYPE_NEXUS_PASSWORD")
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

    apply(plugin = "signing")
    afterEvaluate {
        configure<SigningExtension> {
            useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PRIVATE_KEY_PASSWORD"))
            val publicationsContainer = (extensions.getByName("publishing") as PublishingExtension).publications
            sign(publicationsContainer)
        }
        tasks.withType(Sign::class.java).configureEach {
            isEnabled = !System.getenv("GPG_PRIVATE_KEY").isNullOrBlank()
        }
    }
}
