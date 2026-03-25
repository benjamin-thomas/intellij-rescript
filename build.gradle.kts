import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "com.github.benjamin_thomas"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
        plugin("com.redhat.devtools.lsp4ij:0.19.2")
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

val generateReScriptLexer by tasks.registering(org.jetbrains.grammarkit.tasks.GenerateLexerTask::class) {
    sourceFile.set(file("src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScript.flex"))
    targetOutputDir.set(file("src/main/gen/com/github/benjamin_thomas/intellij_rescript/lang"))
}

sourceSets["main"].java.srcDirs("src/main/gen")

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        dependsOn(generateReScriptLexer)
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
        dependsOn(generateReScriptLexer)
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
        untilBuild.set("253.*")
    }
}
