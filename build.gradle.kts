import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.9.0"
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
        plugin("com.redhat.devtools.lsp4ij:0.13.0")
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildSearchableOptions {
        enabled = false
    }

    // Usage: IDEA_JVM_ARGS="-Dsun.java2d.uiScale.enabled=false" ./gradlew runIde
    runIde {
        System.getenv("IDEA_JVM_ARGS")?.split(" ")?.let { jvmArgs(it) }
    }

    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("253.*")
    }
}
