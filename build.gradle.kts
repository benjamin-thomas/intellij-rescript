import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.jetbrains.grammarkit") version "2023.3.0.3"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = "com.github.benjamin_thomas"
version = "0.4.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
        bundledModule("intellij.spellchecker")
        plugin("com.redhat.devtools.lsp4ij:0.19.2")
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

grammarKit {
    tasks {
        generateLexer {
            sourceFile.set(file("src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScript.flex"))
            targetOutputDir.set(file("src/main/gen/com/github/benjamin_thomas/intellij_rescript/lang"))
        }
        generateParser {
            sourceFile.set(file("src/main/grammars/ReScript.bnf"))
            targetRootOutputDir.set(file("src/main/gen"))
            pathToParser.set("com/github/benjamin_thomas/intellij_rescript/lang/ReScriptParser.java")
            pathToPsiRoot.set("com/github/benjamin_thomas/intellij_rescript/lang/psi")
        }
    }
}

sourceSets["main"].java.srcDirs("src/main/gen")

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        dependsOn("generateLexer", "generateParser")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
        dependsOn("generateLexer", "generateParser")
    }

    buildSearchableOptions {
        enabled = false
    }

    // Usage: IDEA_JVM_ARGS="-Dsun.java2d.uiScale.enabled=false" IDEA_PROJECT=~/code/github.com/benjamin-thomas/7guis/rescript-7guis ./gradlew runIde
    runIde {
        System.getenv("IDEA_JVM_ARGS")?.split(" ")?.let { jvmArgs(it) }
        System.getenv("IDEA_PROJECT")?.let { projectPath ->
            argumentProviders += CommandLineArgumentProvider { listOf(projectPath) }
        }
    }

    patchPluginXml {
        sinceBuild.set("253")
        changeNotes.set(provider {
            with(project.changelog) {
                renderItem(
                    (getOrNull("v${project.version}") ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }
}

changelog {
    version.set("v${project.version}")
    headerParserRegex.set("""v\d+\.\d+\.\d+""".toRegex())
    groups.empty()
}

intellijPlatform {
    pluginVerification {
        ides {
            recommended()
        }
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}
