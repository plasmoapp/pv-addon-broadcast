plugins {
    java
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.crowdin) apply false
    alias(libs.plugins.plasmovoice) apply false
    alias(libs.plugins.plasmovoice.java.templates)
}

tasks {
    jar {
        dependsOn(project(":jar").tasks.build)

        from(project(":jar").sourceSets.main.get().output)
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        annotationProcessor(rootProject.libs.lombok)
    }

    tasks {
        java {
            toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://repo.plasmoverse.com/snapshots")
        maven("https://repo.plasmoverse.com/releases")
    }
}
